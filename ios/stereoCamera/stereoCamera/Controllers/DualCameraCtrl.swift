/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implements the view controller for the camera interface.
*/

import UIKit
import AVFoundation
import Photos
import MetalKit
import CoreMedia


protocol DualCameraController
{
    func getZoom() -> Float
    func setZoom(_ zoom:Float)
    func getOffset() -> CGPoint
    func setOffset(_ offset:CGPoint)
    func configureSession() -> Bool
    func setCameraPair(pair: DualCameraCtrl.CameraPair)
    func viewWillAppear()
    func getSyncedFrames(callback:@escaping (_ left:CVPixelBuffer, _ right:CVPixelBuffer) -> Void)
    func getAudioSettings() -> [String: Any]?
    func getVideoSettings() -> [String: Any]?
    func sliderChanged(value: Float, target: AdjustmentItem)
}

class AdjustmentItem : PopupButtonModalPickerItem
{
    var text: String {
        get { return title }
    }
    
    enum AdjustmentType {
        case VERTICAL_OFFSET
        case ZOOM
    }
    
    let title:String
    let type:AdjustmentType
    let min:Float
    let max:Float
    let currentValue:()->Float
    
    init(title:String, type:AdjustmentType, min:Float, max:Float, currentValue:@escaping ()->Float) {
        self.title = title
        self.type = type
        self.min = min
        self.max = max
        self.currentValue = currentValue
    }
}

class DualCameraCtrl: UIViewController
{
    struct CameraPair{
        let left:AVCaptureDevice
        let right:AVCaptureDevice
    }
    
    var cameraPairs:[CameraPair] = Array()
    
    private let maxZoom = 3.0
    let sessionQueue = DispatchQueue(label: "session queue") // Communicate with the session and other session objects on this queue.
    var cameraCtrl:DualCameraController?
    let angleCalculator = AngleCalculator()
    
    
    // MARK: View Controller Life Cycle
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        
        let controlPanel = root?.viewWithTag(6)
        
        guard let dblCameraPreview = doubleCameraPreview,
              let ctrlPanel = controlPanel,
              let horizRoot = horizontalRoot
        else { return }
        
        let height = horizRoot.frame.height
        let maxWidth = ctrlPanel.frame.origin.x
        let proposedWidth = height * 2.0
        let constraintHeight:CGFloat
        let constraintWidth:CGFloat
        
        if (proposedWidth <= maxWidth) {
            constraintWidth = proposedWidth
            constraintHeight = height
        } else {
            let maxHeight = maxWidth / 2.0
            constraintHeight = maxHeight
            constraintWidth = maxWidth
        }
        
        for constraint in dblCameraPreview.constraints {
            if constraint.firstAttribute == NSLayoutConstraint.Attribute.width {
                constraint.constant = constraintWidth
            } else if constraint.firstAttribute == NSLayoutConstraint.Attribute.height {
                constraint.constant = constraintHeight
            }
        }
        
        dblCameraPreview.setNeedsLayout()
        ctrlPanel.setNeedsLayout()
    }
    
    override public var shouldAutorotate: Bool {
        return true
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        
        doubleCameraPreview?.resumeDrawing()
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        
        doubleCameraPreview?.stopDrawing()
    }
    
    private var doubleCameraPreview: VideoPreviewDouble?
    private var typePickerBtn:PopupButtonPicker?
    private var playbackLbl:UILabel?
    private var shutterBtn:UIButton?
    private var galleryBtn:GalleryBtn?
    private var settingsBtn:UIButton?
    private var adjustBtn:PopupButtonPicker?
    private var slider:UISlider?
    private var shutterView:UIView?
    private var adjustView:UIView?
    private var cancelBtn:UIButton?
    private var saveBtn:UIButton?
    private var debugBtn:UIButton?
    
    private var horizontalRoot:UIView?
    private var verticalRoot:UIView?
    private var root:UIView?
    
    private func setupViews()
    {        
        horizontalRoot = view.viewWithTag(20)
        verticalRoot = view.viewWithTag(30)
        
        if (forcedOrientation == .portrait || forcedOrientation == .portraitUpsideDown) {
            root = verticalRoot
            verticalRoot?.isHidden = false
            horizontalRoot?.isHidden = true
        } else {
            root = horizontalRoot
            verticalRoot?.isHidden = true
            horizontalRoot?.isHidden = false
        }
        
        doubleCameraPreview = root?.viewWithTag(1) as? VideoPreviewDouble
        typePickerBtn = root?.viewWithTag(2) as? PopupButtonPicker
        adjustBtn = root?.viewWithTag(8) as? PopupButtonPicker
        playbackLbl = root?.viewWithTag(3) as? UILabel
        shutterBtn = root?.viewWithTag(4) as? UIButton
        galleryBtn = root?.viewWithTag(5) as? GalleryBtn
        settingsBtn = root?.viewWithTag(6) as? UIButton
        slider = root?.viewWithTag(7) as? UISlider
        shutterView = root?.viewWithTag(10)
        adjustView = root?.viewWithTag(9)
        saveBtn = root?.viewWithTag(11) as? UIButton
        cancelBtn = root?.viewWithTag(12) as? UIButton
        debugBtn = root?.viewWithTag(13) as? UIButton
        
        adjustView?.isHidden = true
        slider?.isHidden = true
        shutterBtn?.addTarget(self, action: #selector(shutterClicked), for: .primaryActionTriggered)
        cancelBtn?.addTarget(self, action: #selector(cancelClicked), for: .primaryActionTriggered)
        saveBtn?.addTarget(self, action: #selector(saveClicked), for: .primaryActionTriggered)
        debugBtn?.isHidden = true
    }
    
    var forcedOrientation:UIInterfaceOrientation!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupViews()

        navigationController?.setNavigationBarHidden(true, animated: false)
        
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        
        let sess:AVCaptureDevice.DiscoverySession
        if #available(iOS 13.0, *) {
            sess = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInTelephotoCamera, .builtInWideAngleCamera, .builtInUltraWideCamera, .builtInDualCamera, .builtInDualWideCamera, .builtInTripleCamera, .builtInTrueDepthCamera], mediaType: .video, position: .back)
            
            let pairs = sess.supportedMultiCamDeviceSets
            for pair in pairs {
                if (pair.count == 2) {
                    let pist = pair.sorted(by: { _,_ in return true })
                    if pist[0].formats[0].videoFieldOfView < pist[1].formats[0].videoFieldOfView {
                        cameraPairs.append(CameraPair(left: pist[0], right: pist[1]))
                    } else {
                        cameraPairs.append(CameraPair(left: pist[1], right: pist[0]))
                    }
                }
            }
        } else {
            sess = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInTelephotoCamera, .builtInWideAngleCamera], mediaType: .video, position: .back)
            
            
            let list = sess.devices
            //back tele, back ; back tele, back ultra ; back ultra, back ;
            //top - back, tele
            //bottom - tele
            //side - ultrawide, tele
        }
        
        guard (cameraPairs.count >= 1)  else {
            print ("phone needs multiple cameras to function")
            return
        }
        
        
        /*
        Configure the capture session.
        In general it is not safe to mutate an AVCaptureSession or any of its
        inputs, outputs, or connections from multiple threads at the same time.
        
        Don't do this on the main queue, because AVCaptureMultiCamSession.startRunning()
        is a blocking call, which can take a long time. Dispatch session setup
        to the sessionQueue so as not to block the main queue, which keeps the UI responsive.
        */
        sessionQueue.async {
            if #available(iOS 13, *) {
                self.cameraCtrl = DualCameraMultiCameraCtrl(dualCameraCtrl: self)
                let result = self.cameraCtrl?.configureSession() ?? false
                
                if !result {
                    self.cameraCtrl = DualCameraLegacyCameraCtrl(dualCameraCtrl: self)
                    let result2 = self.cameraCtrl?.configureSession() ?? false
                    
                    if !result2 {
                        self.cameraCtrl = nil
                    }
                }
            } else {
                self.cameraCtrl = nil
            }
            
            if (self.cameraCtrl == nil) {
                return
            }
            
            self.setCameraPair(0)
            
            if self.viewWillAppearFlag {
                self.cameraCtrl?.viewWillAppear()
            }
        }
        
        // Keep the screen awake
        UIApplication.shared.isIdleTimerDisabled = true
        
        doubleCameraPreview?.frameListener = {[weak self] frame in
            guard let self = self,
                  let bufferPool = self.bufferPool,
                  let videoProc = self.videoProc
            else { return }
            
            if (videoProc.isRecording) {
                var buf:CVPixelBuffer?
            
                CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, bufferPool, &buf)
                guard let img = CIImage.init(mtlTexture: frame, options: nil) else { return }
                
                let gammaFilter = GammaFilter(value: 2.2)
                guard let gammaImg = gammaFilter.update(img),
                      let buffer = buf
                else { return }
                
                let ctx = CIContext()
                ctx.render(gammaImg, to: buffer)
                videoProc.recordVideo(pixelBuffer: buffer, timing: self.videoFrameTiming)
            }
        }
        
        
        class TypePickerDelegate : PopupButtonDelegate
        {
            let parent: DualCameraCtrl
            init(parent: DualCameraCtrl, target: PopupButtonPicker) {
                self.parent = parent
                super.init(target: target)
            }
            
            override func itemSelected(_ button: PopupButton, value: Any?) {
                parent.updateResolution()
            }
        }
        
        guard let typePickerBtn = typePickerBtn else { return }
        typePickerBtn.rootView = view
        typePickerBtn.delegate = TypePickerDelegate(parent: self, target: typePickerBtn)
        guard let typePickerModal = typePickerBtn.createModal() as? PopupButtonModalPicker else { return }
        typePickerModal.valueList = pickerViewArray
        typePickerBtn.modalOrginOffset = CGPoint(x: 0, y: 0)
        
        
        class AdjustDelegate : TypePickerDelegate
        {
            override func getTitle(selected: PopupButtonModalPickerItem) -> String {
                return "< Adjustments"
            }
            
            override func itemSelected(_ button: PopupButton, value: Any?) {
                guard let obj = value as? AdjustmentItem,
                    let slider = parent.slider
                else { return }
                
                slider.isHidden = false
                slider.minimumValue = obj.min
                slider.maximumValue = obj.max
                slider.value = obj.currentValue()
                parent.sliderOriginal = slider.value
                parent.sliderTarget = obj
                
                parent.shutterView?.isHidden = true
                parent.adjustView?.isHidden = false
                slider.isHidden = false
            }
        }
        
        guard let adjustBtn = adjustBtn else { return }
        adjustBtn.rootView = view
        adjustBtn.delegate = AdjustDelegate(parent: self, target: adjustBtn)
        guard let adjustModal = adjustBtn.createModal() as? PopupButtonModalPicker else { return }
        adjustModal.valueList = adjustmentsArray
        adjustBtn.modalOrginOffset = CGPoint(x: 0, y: 0)
        
        slider?.addTarget(self, action: #selector(sliderChanged), for: .valueChanged)
        
        playbackLbl?.text = ""
        
        
        doubleCameraPreview?.initialize()
        updateResolution()
    }
    
    var sliderTarget:AdjustmentItem? = nil
    var sliderOriginal:Float = 1.0
    @objc func sliderChanged() {
        guard let sliderTarget = sliderTarget,
              let slider = slider,
              let cameraCtrl = cameraCtrl
        else { return }
        
        let value = slider.value
        cameraCtrl.sliderChanged(value: value, target: sliderTarget)
    }
    
    struct CameraTypeItem : PopupButtonModalPickerItem
    {
        var text: String {
            get { return title }
        }
        
        enum CameraType {
            case CAMERA
            case VIDEO
        }
        
        let title:String
        let type:CameraType
    }
    
    var pickerViewArray:[CameraTypeItem] = [
        CameraTypeItem(title: "Photo", type: .CAMERA),
        CameraTypeItem(title: "Video", type: .VIDEO)
    ]
    
    var adjustmentsArray:[AdjustmentItem] = [
        AdjustmentItem(title: "Vertical Offset", type: .VERTICAL_OFFSET, min: -100, max: 100, currentValue: { return Float(Cookie.instance.verticalOffset) }),
        AdjustmentItem(title: "Zoom", type: .ZOOM, min: 1.0, max: 4.0,
                       currentValue: { return Cookie.instance.zoom ?? 1.0 })
    ]
    
    private var currentCameraPair:Int = -1
    
    private func setCameraPair(_ index:Int) {
        guard (index >= 0 && index < cameraPairs.count) else { return }
        guard (index != currentCameraPair) else { return }
        
        currentCameraPair = index
        
        let pair = cameraPairs[index]
        print ("switching to left: \(pair.left.localizedName) and right: \(pair.right.localizedName)")
        
        cameraCtrl?.setCameraPair(pair: pair)
    }
    
    private var viewWillAppearFlag:Bool = false
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        viewWillAppearFlag = true
        
        if (cameraCtrl != nil) {
            cameraCtrl?.viewWillAppear()
        }
    }
    
    private func cleanUp()
    {
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        cleanUp()
        super.viewWillDisappear(animated)
    }
    
    @objc // Expose to Objective-C for use with #selector()
    private func didEnterBackground(notification: NSNotification) {
        cleanUp()
    }
    
    @objc // Expose to Objective-C for use with #selector()
    func willEnterForground(notification: NSNotification) {

    }
    
    // MARK: KVO and Notifications
    
    private func addObservers() {
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(didEnterBackground),
                                               name: NSNotification.Name.UIApplicationDidEnterBackground,
                                               object: nil)
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(willEnterForground),
                                               name: NSNotification.Name.UIApplicationWillEnterForeground,
                                               object: nil)
    }
    
    
    // MARK: Capture Session Management
    
    enum SessionSetupResult {
        case success
        case notAuthorized
        case configurationFailed
        case multiCamNotSupported
    }
    
    private var zoomLeft:CMSampleBuffer?
    private var zoomRight:CMSampleBuffer?
    private var zoomCalculated = true
    private var frameCounter = Date()
    
    
    private var videoFrameTiming = CMSampleTimingInfo()
    private var videoDescription:CMFormatDescription?
    
    func captureOutput(didOutput sampleBuffer: CMSampleBuffer, isLeft:Bool, zoom: Float, offset: CGPoint) {
        if let videoProc = videoProc, videoProc.isRecording {
            let now = Date()
            let diff = now.timeIntervalSince1970 - recordLabelStart.timeIntervalSince1970
            let text = diff.formattedString("hh:mm:ss.ms")
            
            DispatchQueue.main.async { [weak self] in
                self?.playbackLbl?.text = text
            }
        }
        
        let angle = angleCalculator.calculate()
        
        if isLeft {
            videoDescription = CMSampleBufferGetFormatDescription(sampleBuffer)
            CMSampleBufferGetSampleTimingInfo(sampleBuffer, 0, &videoFrameTiming)
            doubleCameraPreview?.rotation = Float(angle)
            doubleCameraPreview?.renderBuffer(sampleBuffer: sampleBuffer, side: .LEFT, zoom: zoom, offset: offset)
        }
        else {
            doubleCameraPreview?.renderBuffer(sampleBuffer: sampleBuffer, side: .RIGHT, zoom: zoom, offset: offset)
        }
    }
    
    func captureOutput(audioOutput sampleBuffer: CMSampleBuffer) {
        videoProc?.recordAudio(sampleBuffer: sampleBuffer)
    }
    
    private func updateResolution() {
        guard let captureType = typePickerBtn?.pickedItem as? CameraTypeItem else { return }
        let qualityVal:Int
        if (captureType.type == .CAMERA) {
            qualityVal = Cookie.instance.photoImageQuality.toInt()
        } else {
            qualityVal = Cookie.instance.videoImageQuality.toInt()
        }
        
        let resolution = CGSize(width: qualityVal * 2, height: qualityVal)
        let procType = Cookie.instance.videoFormat
        doubleCameraPreview?.setupProcessor(type: procType, size: resolution)
        
        let attr = [kCVPixelBufferMetalCompatibilityKey: true,
                    kCVPixelBufferPixelFormatTypeKey: kCVPixelFormatType_32BGRA,
                    kCVPixelBufferWidthKey: resolution.width,
                    kCVPixelBufferHeightKey: resolution.height
                    ] as CFDictionary
        CVPixelBufferPoolCreate(kCFAllocatorDefault, nil, attr, &bufferPool)
    }
    
    @objc func shutterClicked(_ sender: Any) {
        guard let item = typePickerBtn?.selectedItem as? CameraTypeItem else { return }
        if (item.type == .CAMERA) {
            capturePhoto()
        } else {
            videoTapped()
        }
    }
    
    @objc func saveClicked(_ sender: Any) {
        shutterView?.isHidden = false
        adjustView?.isHidden = true
        slider?.isHidden = true
        
        guard let value = slider?.value else { return }
        if (sliderTarget?.type == .VERTICAL_OFFSET) {
            Cookie.instance.verticalOffset = CGFloat(value)
        } else if (sliderTarget?.type == .ZOOM) {
            Cookie.instance.zoom = value
        }
    }
    
    @objc func cancelClicked(_ sender: Any) {
        shutterView?.isHidden = false
        adjustView?.isHidden = true
        slider?.isHidden = true
        
        guard let sliderTarget = sliderTarget
        else { return }
        
        cameraCtrl?.sliderChanged(value: sliderOriginal, target: sliderTarget)
    }
    
    private var videoProc:VideoProcessor?
    //private var videoProc:MovieRecorder?
    private var bufferPool:CVPixelBufferPool?
    private var shutterColor = UIColor.black
    
    func videoTapped() {
        if (videoProc != nil && videoProc!.isRecording) {
            videoProc!.stop()
            self.videoProc = nil
            self.shutterBtn?.tintColor = self.shutterColor
            self.playbackLbl?.text = ""
        } else {
            let procHeight = Cookie.instance.videoImageQuality.toInt()
            let procSize = CGSize(width: procHeight * 2, height: procHeight)
            videoProc = VideoProcessor(size: procSize)
            
            shutterColor = shutterBtn?.tintColor ?? UIColor.white
            shutterBtn?.tintColor = UIColor.red
            
            if let audioSettings = cameraCtrl?.getAudioSettings(),
               var videoSettings = cameraCtrl?.getVideoSettings(),
               let videoDescription = videoDescription {
                let sz = videoProc!.frameSize
                videoSettings[AVVideoWidthKey] = Int(sz.width)
                videoSettings[AVVideoHeightKey] = Int(sz.height)
                
                recordLabelStart = Date()
                videoProc!.start(audioSettings: audioSettings, videoSettings: videoSettings, videoDescription: videoDescription)
            }
        }
    }
    
    private var recordLabelStart = Date()
    
    private func capturePhoto() {
        self.cameraCtrl?.getSyncedFrames(callback: {[weak self] (left, right) in
            self?.shutterClicked2(left, right)
        })
    }
    
    private var loaderCtrl:LoadingPopupCtrl?
    private var loaderMessage:String = "Saving ..."
    
    @IBAction func captureTypeTapped(_ sender: Any) {
    }
    
    @IBAction func measureClicked(_ sender: Any) {
        self.cameraCtrl?.getSyncedFrames(callback: {[weak self] (left, right) in
            
            self?.measureClicked2(left, right)
        })
        
        //showLoader(true)
    }
    
    func showLoader(_ show:Bool, message:String? = nil)
    {
        DispatchQueue.main.async
        {
            [unowned self] in
            if (message != nil)
                { loaderMessage = message! }
            
            if (show && loaderCtrl == nil)
            {
                loaderCtrl = LoadingPopupCtrl.initFromStoryboard()
                
                loaderCtrl!.header = loaderMessage
                
                present(loaderCtrl!, animated: true, completion: nil)
            }
            else if (!show && loaderCtrl != nil)
            {
                loaderCtrl = nil
                dismiss(animated: false, completion: nil)
            }
        }
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        if (segue.identifier == "measureSegue"){
            let vc = segue.destination as! MeasureCtrl
            let arr = sender as! [CVPixelBuffer]
            vc.leftPixels = arr[0]
            vc.rightPixels = arr[1]
            vc.zoom = Cookie.instance.zoom ?? 1.0
        } else if (segue.identifier == "editorSegue"){
            let arr = sender as! [CVPixelBuffer]
            let vc = segue.destination as! ImageEditorCtrl
            vc.leftData = ImageEditorData(origData: arr[0], zoom: 1.0, rotation: 0.0, offset: CGPoint())
            vc.rightData = ImageEditorData(origData: arr[1], zoom: CGFloat(Cookie.instance.zoom ?? 1.0), rotation: 0.0, offset: CGPoint())
        }
    }
    
    private func measureClicked2(_ lPixelBuffer:CVPixelBuffer, _ rPixelBuffer:CVPixelBuffer)
    {
        //self.showLoader(false)
        //sleep(100)
        
        DispatchQueue.main.async {
            self.performSegue(withIdentifier: "editorSegue", sender: [lPixelBuffer, rPixelBuffer])
        }
        
        /*
        DispatchQueue.main.async {
            self.performSegue(withIdentifier: "measureSegue", sender: [lPixelBuffer, rPixelBuffer])
        }
        */
    }
    
    private let saver = Files.instance
    
    private func shutterClicked2(_ lPixelBuffer:CVPixelBuffer, _ rPixelBuffer:CVPixelBuffer)
    {
        guard let ctrl = cameraCtrl else { return }
        
        let lRot = angleCalculator.calculate()
        
        let leftData = ImageEditorData(origData: lPixelBuffer, zoom: 1.0, rotation: Float(lRot), offset: CGPoint())
        let rightData = ImageEditorData(origData: rPixelBuffer, zoom: CGFloat(ctrl.getZoom()), rotation: Float(lRot), offset: ctrl.getOffset())
        
        let quality = Cookie.instance.photoImageQuality
        let exportTypes = Cookie.instance.photoFormat
        
        for type in exportTypes {
            let exporter = ImageExporter(leftData: leftData, rightData: rightData, processType: type, outputQuality: quality)
            exporter.export()
        }
        
        showLoader(false)
    }
    
    private var current = -1
    private func nextTexture(proc: ImageProcessor) -> MTLTexture {
        current = (current + 1) % 3;
        switch(current){
        case 0: return proc._inTexture!
        case 1: return proc._midTexture!
        default: return proc._outTexture!
        }
    }
    
    public func alert(title: String, message: String, actions: [UIAlertAction]) {
        let alertController = UIAlertController(title: title,
                                                message: message,
                                                preferredStyle: .alert)
        
        actions.forEach {
            alertController.addAction($0)
        }
        
        self.present(alertController, animated: true, completion: nil)
    }
}

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
import CoreMotion


protocol DualCameraController
{
    func getZoom() -> Float
    func setZoom(_ zoom:Float)
    func configureSession() -> Bool
    func setCameraPair(pair: DualCameraCtrl.CameraPair)
    func viewWillAppear()
    func getSyncedFrames(callback:@escaping (_ left:CVPixelBuffer, _ right:CVPixelBuffer) -> Void)
    func getAudioSettings() -> [String: Any]?
    func getVideoSettings() -> [String: Any]?
}

class DualCameraCtrl: UIViewController, PopupButtonDelegate
{
    struct CameraPair{
        let left:AVCaptureDevice
        let right:AVCaptureDevice
    }
    
    var cameraPairs:[CameraPair] = Array()
    
    private let maxZoom = 3.0
    let sessionQueue = DispatchQueue(label: "session queue") // Communicate with the session and other session objects on this queue.
    var cameraCtrl:DualCameraController?
    
    
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
        playbackLbl = root?.viewWithTag(3) as? UILabel
        shutterBtn = root?.viewWithTag(4) as? UIButton
        galleryBtn = root?.viewWithTag(5) as? GalleryBtn
        settingsBtn = root?.viewWithTag(6) as? UIButton
        
        
        shutterBtn?.addTarget(self, action: #selector(shutterClicked), for: .primaryActionTriggered)
    }
    
    var forcedOrientation:UIInterfaceOrientation!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupViews()

        navigationController?.setNavigationBarHidden(true, animated: false)
        
        doubleCameraPreview?.initialize(size:  CGSize(width: CGFloat(VideoProcessor.ENCODED_WIDTH), height: CGFloat(VideoProcessor.ENCODED_HEIGHT)))
        
        
        motion.startAccelerometerUpdates()
        
        let attr = [kCVPixelBufferMetalCompatibilityKey: true,
                    kCVPixelBufferPixelFormatTypeKey: kCVPixelFormatType_32BGRA,
                    kCVPixelBufferWidthKey: VideoProcessor.ENCODED_WIDTH,
                    kCVPixelBufferHeightKey: VideoProcessor.ENCODED_HEIGHT
                    ] as CFDictionary
        CVPixelBufferPoolCreate(kCFAllocatorDefault, nil, attr, &bufferPool)
        
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
        
        
        guard let typePickerBtn = typePickerBtn else { return }
        typePickerBtn.rootView = view
        typePickerBtn.delegate = self
        guard let typePickerModal = typePickerBtn.createModal() as? PopupButtonModalPicker else { return }
        typePickerModal.valueList = pickerViewArray
        typePickerBtn.modalOrginOffset = CGPoint(x: 0, y: 0)
        
        playbackLbl?.text = ""
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
        
        motion.startAccelerometerUpdates()
    }
    
    private func cleanUp()
    {
        motion.stopAccelerometerUpdates()
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
    private let motion = CMMotionManager()
    private var motionArr:[Double] = Array(repeating: 0.0, count: 5)
    
    private func calculateAngle() -> Double {
        var angle:Double
        
        if motion.isAccelerometerAvailable,
           let accel = motion.accelerometerData
        {
            let x = accel.acceleration.x
            let y = accel.acceleration.y
            let z = accel.acceleration.z
            
            let xs = NSString(format:"%.2f", x)
            let ys = NSString(format:"%.2f", y)
            let zs = NSString(format:"%.2f", z)
            //print ("\(xs), \(ys), \(zs)")
            
            let threshold = 0.08
            
            if (abs(x) < threshold) {
                angle = (y < 0) ? 0.0 : Double.pi
            } else {
                angle = atan2(x, y)
                
                if (x < 0) {
                    angle += 2.0 * Double.pi
                }
                
                angle -= Double.pi
            }
            
        } else {
            angle = 0.0
        }
        
        //angle += Double.pi / 2.0
        
        motionArr.removeFirst()
        motionArr.append(angle)
        
        var total = 0.0
        for item in motionArr {
            total += item
        }
        
        return total / Double(motionArr.count)
    }
    
    private var videoFrameTiming = CMSampleTimingInfo()
    private var videoDescription:CMFormatDescription?
    
    func captureOutput(didOutput sampleBuffer: CMSampleBuffer, isLeft:Bool) {
        if let videoProc = videoProc, videoProc.isRecording {
            let now = Date()
            let diff = now.timeIntervalSince1970 - recordLabelStart.timeIntervalSince1970
            let text = diff.formattedString("hh:mm:ss.ms")
            
            DispatchQueue.main.async { [weak self] in
                self?.playbackLbl?.text = text
            }
        }
        
        let angle = calculateAngle()
        
        if isLeft {
            videoDescription = CMSampleBufferGetFormatDescription(sampleBuffer)
            CMSampleBufferGetSampleTimingInfo(sampleBuffer, 0, &videoFrameTiming)
            doubleCameraPreview?.rotation = Float(angle)
            doubleCameraPreview?.renderBuffer(sampleBuffer: sampleBuffer, side: .LEFT)
        }
        else {
            doubleCameraPreview?.renderBuffer(sampleBuffer: sampleBuffer, side: .RIGHT)
        }
    }
    
    func captureOutput(audioOutput sampleBuffer: CMSampleBuffer) {
        videoProc?.recordAudio(sampleBuffer: sampleBuffer)
    }
    
    func itemSelected(_ button: PopupButton, value: Any?) {
        print("camera type selected")
    }
    
    @objc func shutterClicked(_ sender: Any) {
        guard let item = typePickerBtn?.selectedItem as? CameraTypeItem else { return }
        if (item.type == .CAMERA) {
            capturePhoto()
        } else {
            videoTapped()
        }
    }
    
    private var videoProc:VideoProcessor?
    //private var videoProc:MovieRecorder?
    private var bufferPool:CVPixelBufferPool?
    private var shutterColor = UIColor.black
    
    func videoTapped() {
        if (videoProc == nil){
            videoProc = VideoProcessor()
        }
        
        if (!videoProc!.isRecording) {
            shutterColor = shutterBtn?.tintColor ?? UIColor.white
            shutterBtn?.tintColor = UIColor.red
            
            if let audioSettings = cameraCtrl?.getAudioSettings(),
               let videoSettings = cameraCtrl?.getVideoSettings(),
               let videoDescription = videoDescription {
                recordLabelStart = Date()
                videoProc!.start(audioSettings: audioSettings, videoSettings: videoSettings, videoDescription: videoDescription)
            }
        } else {
            videoProc!.stop()
            self.videoProc = nil
            self.shutterBtn?.tintColor = self.shutterColor
            self.playbackLbl?.text = ""
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
        }
    }
    
    private func measureClicked2(_ lPixelBuffer:CVPixelBuffer, _ rPixelBuffer:CVPixelBuffer)
    {
        //self.showLoader(false)
        //sleep(100)
        
        DispatchQueue.main.async {
            self.performSegue(withIdentifier: "measureSegue", sender: [lPixelBuffer, rPixelBuffer])
        }
        
    }
    
    private let saver = Files.instance
    
    private func shutterClicked2(_ lPixelBuffer:CVPixelBuffer, _ rPixelBuffer:CVPixelBuffer)
    {
        let zoom:Float = 1.0
        let lRot = calculateAngle()
        let leftData = ImageEditorData(origData: lPixelBuffer, zoom: zoom, rotation: Float(lRot))
        let rightData = ImageEditorData(origData: rPixelBuffer, zoom: zoom, rotation: Float(lRot))
        
        let exporter = ImageExporter(leftData: leftData, rightData: rightData)
        exporter.export()
        
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

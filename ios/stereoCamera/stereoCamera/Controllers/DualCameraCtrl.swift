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
    func configureSession()
    func viewWillAppear()
}

class DualCameraCtrl: UIViewController
{
    @IBOutlet weak var leftCameraPreview: VideoPreview!
    @IBOutlet weak var rightCameraPreview: VideoPreview!
    @IBOutlet weak var shutterBtn: UIButton!
    @IBOutlet weak var zoomSlider: UISlider!
    @IBOutlet weak var galleryBtn: GalleryBtn!
    
    private let zoomFinder = ZoomFinder()
    
    private let maxZoom = 4.0
    let sessionQueue = DispatchQueue(label: "session queue") // Communicate with the session and other session objects on this queue.
    var cameraCtrl:DualCameraController?
    
    
    // MARK: View Controller Life Cycle
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        leftCameraPreview.resumeDrawing()
        rightCameraPreview.resumeDrawing()
        zoomSlider.value = cameraCtrl?.getZoom() ?? 1.0
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        
        leftCameraPreview.stopDrawing()
        rightCameraPreview.stopDrawing()
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
                    
        leftCameraPreview?.initialize()
        rightCameraPreview?.initialize()
        
        
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        
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
            } else {
                return
            }
            
            self.cameraCtrl?.configureSession()
            
            if self.viewWillAppearFlag {
                self.cameraCtrl?.viewWillAppear()
            }
        }
        
        // Keep the screen awake
        UIApplication.shared.isIdleTimerDisabled = true
        
        zoomSlider.minimumValue = 1.0
        zoomSlider.maximumValue = Float(maxZoom)
        zoomSlider.isContinuous = true
        zoomSlider.value = self.cameraCtrl?.getZoom() ?? 1.0
        zoomUpdated(self)
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
        Cookie.instance.setZoomForDual(zoom: zoomSlider.value)
        
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
    
    private var shutterProcessing = false
    private var shutterWaiting = false
    private var shutterLeft:CMSampleBuffer?
    private var shutterRight:CMSampleBuffer?
    
    private var zoomLeft:CMSampleBuffer?
    private var zoomRight:CMSampleBuffer?
    private var zoomCalculated = true
    private var frameCounter = Date()
    
    func captureOutput(didOutput sampleBuffer: CMSampleBuffer, isLeft:Bool) {
        if (shutterWaiting) {
            guard shutterProcessing == false else { return }
            if isLeft {
                shutterLeft = sampleBuffer
            } else {
                shutterRight = sampleBuffer
            }
            
            if (shutterLeft != nil && shutterRight != nil) {
                shutterProcessing = true
                shutterClicked2(shutterLeft!, shutterRight!)
            }
        } else {
            if isLeft {
                leftCameraPreview.renderBuffer(sampleBuffer: sampleBuffer)
            }
            else {
                rightCameraPreview.renderBuffer(sampleBuffer: sampleBuffer)
            }
        }
        
        if (!zoomCalculated && Date().timeIntervalSince(frameCounter) > 1.0) {
            if isLeft {
                zoomLeft = sampleBuffer
            } else {
                zoomRight = sampleBuffer
            }
            
            if (zoomLeft != nil && zoomRight != nil) {
                zoomCalculated = true
                guard let zoomLeft = zoomLeft,
                      let zoomRight = zoomRight,
                      let lPixelBuffer = CMSampleBufferGetImageBuffer(zoomLeft),
                      let rPixelBuffer = CMSampleBufferGetImageBuffer(zoomRight)
                else { return }
                calculateZoom(leftImage: lPixelBuffer, rightImage: rPixelBuffer)
            }
        }
    }
    
    @IBAction func shutterClicked(_ sender: Any) {
        guard shutterProcessing == false && shutterWaiting == false else { return }
        if (shutterWaiting || shutterProcessing) { return }
        
        shutterLeft = nil
        shutterRight = nil
        shutterWaiting = true
        shutterProcessing = false
        
        showLoader(true)
    }
    
    private var loaderCtrl:LoadingPopupCtrl?
    private var loaderMessage:String = "Saving ..."
    
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
    
    private let saver = Files.instance
    
    private func shutterClicked2(_ leftPixels:CMSampleBuffer, _ rightPixels:CMSampleBuffer)
    {
        guard let lPixelBuffer = CMSampleBufferGetImageBuffer(leftPixels) else { return }
        let lw = CVPixelBufferGetWidth(lPixelBuffer)
        let lh = CVPixelBufferGetHeight(lPixelBuffer)
        let lMargin = ImageUtils.findMargins(size: ImageUtils.Size(width: lw, height: lh), zoom: 1.0)
        
        guard let rPixelBuffer = CMSampleBufferGetImageBuffer(rightPixels) else { return }
        let rw = CVPixelBufferGetWidth(rPixelBuffer)
        let rh = CVPixelBufferGetHeight(rPixelBuffer)
        let rMargin = ImageUtils.findMargins(size: ImageUtils.Size(width: rw, height: rh), zoom: 1.0)
        guard let orientation = leftCameraPreview.orientation else { return }
        
        let proc = ImageProcessorSplit(size: ImageUtils.Size(width: Int(rMargin.width) * 2, height: Int(rMargin.height)))
        print("processing")
        
        proc.setPixels(pixels: lPixelBuffer, margins: lMargin, orientation: orientation)
        proc.processCurrentInTexture(.LEFT)
        print("finished processing left side")
        
        proc.setPixels(pixels: rPixelBuffer, margins: rMargin, orientation: orientation)
        proc.processCurrentInTexture(.RIGHT)
        print("finished processing right side")
        
        
        saveProcessedImage(proc: proc)
        
        AudioServicesPlaySystemSound(1108)
        showLoader(false)
        
        shutterWaiting = false
        shutterProcessing = false
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
    
    private func saveProcessedImage(proc: ImageProcessor) {
        guard let img = proc.getOutput() else { return }
        saveProcessedImage(img: img)
    }
    
    func saveProcessedImage(img:CIImage) {
        guard let cs = CGColorSpace(name: CGColorSpace.displayP3) else { return }
        let ctx = CIContext()
        let jpegData = ctx.jpegRepresentation(of: img, colorSpace: cs, options: [:])
        guard let data = jpegData else { return }
        
        saver.saveToPhotos(data: data, onSaved: { savedImg in
            print ("saved successfully")
        })
    }
    
    
    @IBAction func zoomUpdated(_ sender: Any) {
        
        let z = zoomSlider.value
        cameraCtrl?.setZoom(z)
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
    
    func calculateZoom(leftImage: CVImageBuffer, rightImage: CVImageBuffer) {
        zoomFinder.baseHist.setPixels(pixels: leftImage)
        zoomFinder.adjHist.setPixels(pixels: rightImage)
        
        if (zoomFinder.canFindZoom()) {
            zoomFinder.findZoom(max: Float(maxZoom)) { [weak self] (zoom) in
                DispatchQueue.main.async {
                    self?.zoomSlider.value = Float(zoom)
                    self?.zoomUpdated(self as Any)
                }
            }
        }
    }
}

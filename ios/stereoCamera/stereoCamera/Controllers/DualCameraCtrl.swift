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


@available(iOS 13, *)
class DualCameraCtrl: UIViewController,
                      AVCaptureAudioDataOutputSampleBufferDelegate,
                      AVCaptureVideoDataOutputSampleBufferDelegate, AVCapturePhotoCaptureDelegate
{
    
    
    @IBOutlet weak var leftCameraPreview: VideoPreview!
    @IBOutlet weak var rightCameraPreview: VideoPreview!
    @IBOutlet weak var shutterBtn: UIButton!
    @IBOutlet weak var zoomSlider: UISlider!
    @IBOutlet weak var debugPreview: VideoPreview!
    @IBOutlet weak var galleryBtn: GalleryBtn!
    
    private let session = AVCaptureMultiCamSession()
    private var isSessionRunning = false
    private let sessionQueue = DispatchQueue(label: "session queue") // Communicate with the session and other session objects on this queue.
    private let dataOutputQueue = DispatchQueue(label: "data output queue")
    private var setupResult: SessionSetupResult = .success
    
    private var leftCameraStr: CameraStr?
    private var rightCameraStr: CameraStr?
    
    private let zoomFinder = ZoomFinder()
    
    private let maxZoom = 4.0
    
    
    // MARK: View Controller Life Cycle
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        leftCameraPreview.resumeDrawing()
        rightCameraPreview.resumeDrawing()
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
        debugPreview?.initialize()
        debugPreview?.orientation = ImageUtils.CameraOrientation.DEG_90
        
        
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
            self.configureSession()
        }
        
        // Keep the screen awake
        UIApplication.shared.isIdleTimerDisabled = true
        
        zoomSlider.minimumValue = 1.0
        zoomSlider.maximumValue = Float(maxZoom)
        zoomSlider.isContinuous = true
        zoomSlider.value = 1.0
        zoomUpdated(self)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        sessionQueue.async {
            switch self.setupResult {
            case .success:
                // Only setup observers and start the session running if setup succeeded.
                self.addObservers()
                self.session.startRunning()
                self.isSessionRunning = self.session.isRunning
                
            case .notAuthorized:
                DispatchQueue.main.async {
                    let changePrivacySetting = "\("foo") doesn't have permission to use the camera, please change privacy settings"
                    let message = NSLocalizedString(changePrivacySetting, comment: "Alert message when the user has denied access to the camera")
                    let alertController = UIAlertController(title: "foo", message: message, preferredStyle: .alert)
                    
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"),
                                                            style: .cancel,
                                                            handler: nil))
                    
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("Settings", comment: "Alert button to open Settings"),
                                                            style: .`default`,
                                                            handler: { _ in
                                                                if let settingsURL = URL(string: UIApplicationOpenSettingsURLString) {
                                                                    UIApplication.shared.open(settingsURL,
                                                                                              options: [:],
                                                                                              completionHandler: nil)
                                                                }
                    }))
                    
                    self.present(alertController, animated: true, completion: nil)
                }
                
            case .configurationFailed:
                DispatchQueue.main.async {
                    let alertMsg = "Alert message when something goes wrong during capture session configuration"
                    let message = NSLocalizedString("Unable to capture media", comment: alertMsg)
                    let alertController = UIAlertController(title: "foo", message: message, preferredStyle: .alert)
                    
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"),
                                                            style: .cancel,
                                                            handler: nil))
                    
                    self.present(alertController, animated: true, completion: nil)
                }
                
            case .multiCamNotSupported:
                DispatchQueue.main.async {
                    let alertMessage = "Alert message when multi cam is not supported"
                    let message = NSLocalizedString("Multi Cam Not Supported", comment: alertMessage)
                    let alertController = UIAlertController(title: "foo", message: message, preferredStyle: .alert)
                    
                    self.present(alertController, animated: true, completion: nil)
                }
            }
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        sessionQueue.async {
            if self.setupResult == .success {
                self.session.stopRunning()
                self.isSessionRunning = self.session.isRunning
                self.removeObservers()
            }
        }
        
        super.viewWillDisappear(animated)
    }
    
    @objc // Expose to Objective-C for use with #selector()
    private func didEnterBackground(notification: NSNotification) {
        // Free up resources.

    }
    
    @objc // Expose to Objective-C for use with #selector()
    func willEnterForground(notification: NSNotification) {

    }
    
    // MARK: KVO and Notifications
    
    private var sessionRunningContext = 0
    
    private var keyValueObservations = [NSKeyValueObservation]()
    
    private func addObservers() {
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(didEnterBackground),
                                               name: NSNotification.Name.UIApplicationDidEnterBackground,
                                               object: nil)
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(willEnterForground),
                                               name: NSNotification.Name.UIApplicationWillEnterForeground,
                                               object: nil)
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(sessionRuntimeError),
                                               name: .AVCaptureSessionRuntimeError,
                                               object: session)
        
        // A session can run only when the app is full screen. It will be interrupted in a multi-app layout.
        // Add observers to handle these session interruptions and inform the user.
        // See AVCaptureSessionWasInterruptedNotification for other interruption reasons.
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(sessionWasInterrupted),
                                               name: .AVCaptureSessionWasInterrupted,
                                               object: session)
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(sessionInterruptionEnded),
                                               name: .AVCaptureSessionInterruptionEnded,
                                               object: session)
    }
    
    private func removeObservers() {
        for keyValueObservation in keyValueObservations {
            keyValueObservation.invalidate()
        }
        
        keyValueObservations.removeAll()
    }
    
    
    // MARK: Capture Session Management
    
    private enum SessionSetupResult {
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
    private var zoomCalculated = false
    private var frameCounter = Date()
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        if output is AVCaptureVideoDataOutput {
            if (shutterWaiting) {
                guard shutterProcessing == false else { return }
                if (connection == leftCameraStr?.videoDataOutputConnection) {
                    shutterLeft = sampleBuffer
                } else if (connection == rightCameraStr?.videoDataOutputConnection) {
                    shutterRight = sampleBuffer
                }
                
                if (shutterLeft != nil && shutterRight != nil) {
                    shutterProcessing = true
                    shutterClicked2(shutterLeft!, shutterRight!)
                }
            } else {
                if (connection == leftCameraStr?.videoDataOutputConnection) {
                    leftCameraPreview.captureOutput(captureOutput: output, didOutputSampleBuffer: sampleBuffer, fromConnection: connection)
                }
                else if (connection == rightCameraStr?.videoDataOutputConnection) {
                    rightCameraPreview.captureOutput(captureOutput: output, didOutputSampleBuffer: sampleBuffer, fromConnection: connection)
                }
            }
            
            if (!zoomCalculated && Date().timeIntervalSince(frameCounter) > 1.0) {
                if (connection == leftCameraStr?.videoDataOutputConnection) {
                    zoomLeft = sampleBuffer
                } else if (connection == rightCameraStr?.videoDataOutputConnection) {
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
    
    
    
    private var currentZoom:Float = 1.0
    
    @IBAction func zoomUpdated(_ sender: Any) {
        currentZoom = zoomSlider.value
        //rightCameraPreview.zoom = currentZoom
        
        do {
            try rightCameraStr?.deviceInput?.device.lockForConfiguration()
            rightCameraStr?.deviceInput?.device.videoZoomFactor = CGFloat(currentZoom)
            rightCameraStr?.deviceInput?.device.unlockForConfiguration()
        } catch {}
        
    }
    
    // Must be called on the session queue
    private func configureSession() {
        guard setupResult == .success else { return }
        
        guard AVCaptureMultiCamSession.isMultiCamSupported else {
            print("MultiCam not supported on this device")
            setupResult = .multiCamNotSupported
            return
        }
        
        // When using AVCaptureMultiCamSession, it is best to manually add connections from AVCaptureInputs to AVCaptureOutputs
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
            if setupResult == .success {
                checkSystemCost()
            }
        }
        
        
        rightCameraStr = configureCamera(.builtInUltraWideCamera)
        guard rightCameraStr != nil else {
            setupResult = .configurationFailed
            return
        }
        
        
        let rightDim = rightCameraStr?.device?.activeFormat.highResolutionStillImageDimensions.height ?? 1024
        let maxDim = Int32(Float(rightDim) * 0.8)
        leftCameraStr = configureCamera(.builtInWideAngleCamera, maxDim: maxDim)
        guard leftCameraStr != nil else {
            setupResult = .configurationFailed
            return
        }
    }

    struct CameraStr {
        var device:AVCaptureDevice?
        var deviceInput:AVCaptureDeviceInput?
        var videoDataOutput:AVCaptureVideoDataOutput?
        var videoDataOutputConnection:AVCaptureConnection?
        var photoOutput:AVCapturePhotoOutput?
    }
    
    private func configureCamera(_ type:AVCaptureDevice.DeviceType, maxDim:Int32 = Int32.max) -> CameraStr? {
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
        }
        
        // Find the back camera
        guard let backCamera = AVCaptureDevice.default(type, for: .video, position: .back) else {
            print("Could not find the back camera")
            return nil
        }
        
        let deviceInput:AVCaptureDeviceInput
        let videoDataOutput = AVCaptureVideoDataOutput()
        
        // Add the back camera input to the session
        do {
            deviceInput = try AVCaptureDeviceInput(device: backCamera)
            
            guard session.canAddInput(deviceInput) else {
                    print("Could not add back camera device input")
                    return nil
            }
            session.addInputWithNoConnections(deviceInput)
        } catch {
            print("Could not create back camera device input: \(error)")
            return nil
        }
        
        var sorted:[AVCaptureDevice.Format] = []
        let formats = deviceInput.device.formats
        for fmt in formats {
            if fmt.isMultiCamSupported && fmt.formatDescription.dimensions.height < maxDim {
                sorted.append(fmt)
                
                let hiRes = fmt.highResolutionStillImageDimensions
                let preRes = fmt.formatDescription.dimensions
                
                //print ("hi", hiRes.width, "x", hiRes.height, "lo", preRes.width, "x", preRes.height, separator: " ")
            }
        }
        
        sorted.sort { (a, b) -> Bool in
            return a.formatDescription.dimensions.height > b.formatDescription.dimensions.height
        }
        
        let attempt = 0
        
        do {
            try backCamera.lockForConfiguration()
            backCamera.activeFormat = sorted[attempt]
            let fr = sorted[attempt].videoSupportedFrameRateRanges
            let videoMinFrameDurationOverride = CMTimeMake(1, Int32(15))
            backCamera.activeVideoMaxFrameDuration = fr[0].maxFrameDuration
            backCamera.activeVideoMinFrameDuration = videoMinFrameDurationOverride
            backCamera.unlockForConfiguration()
        } catch {
            print ("Could not adjust camera resolution")
            return nil
        }
        
        let fmt = sorted[attempt]
        let hiRes = fmt.highResolutionStillImageDimensions
        let preRes = fmt.formatDescription.dimensions
        
        print ("set camera resoulution to hi", hiRes.width, "x", hiRes.height, "lo", preRes.width, "x", preRes.height, separator: " ")
        
        // Find the back camera device input's video port
        guard let backCameraVideoPort = deviceInput.ports(for: .video,
                  sourceDeviceType: backCamera.deviceType,
                  sourceDevicePosition: backCamera.position).first
        else {
                    print("Could not find the back camera device input's video port")
                    return nil
        }
        
        // Add the back camera video data output
        guard session.canAddOutput(videoDataOutput) else {
            print("Could not add the back camera video data output")
            return nil
        }
        session.addOutputWithNoConnections(videoDataOutput)
        videoDataOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        videoDataOutput.setSampleBufferDelegate(self, queue: dataOutputQueue)
        
        // Connect the back camera device input to the back camera video data output
        let videoDataOutputConnection = AVCaptureConnection(inputPorts: [backCameraVideoPort], output: videoDataOutput)
        guard session.canAdd(videoDataOutputConnection) else {
            print("Could not add a connection to the back camera video data output")
            return nil 
        }
        session.add(videoDataOutputConnection)
        
        /*let stillOutput = AVCapturePhotoOutput()
        guard session.canAddOutput(stillOutput) else {
            print ("Could not add still image capture")
            return nil
        }
        session.addOutputWithNoConnections(stillOutput)
        let stillDataOutputConnection = AVCaptureConnection(inputPorts: [backCameraVideoPort], output: stillOutput)
        guard session.canAdd(stillDataOutputConnection) else {
            print ("Could not add a connection to the back camera still output")
            return nil
        }
        session.add(stillDataOutputConnection)*/
        
        return CameraStr(device: backCamera, deviceInput: deviceInput, videoDataOutput: videoDataOutput, videoDataOutputConnection: videoDataOutputConnection, photoOutput: nil)
    }
    
    @objc // Expose to Objective-C for use with #selector()
    private func sessionWasInterrupted(notification: NSNotification) {
        // In iOS 9 and later, the userInfo dictionary contains information on why the session was interrupted.
        if let userInfoValue = notification.userInfo?[AVCaptureSessionInterruptionReasonKey] as AnyObject?,
            let reasonIntegerValue = userInfoValue.integerValue,
            let reason = AVCaptureSession.InterruptionReason(rawValue: reasonIntegerValue) {
            print("Capture session was interrupted (\(reason))")
            
            if reason == .videoDeviceInUseByAnotherClient {
                // Simply fade-in a button to enable the user to try to resume the session running.
            } else if reason == .videoDeviceNotAvailableWithMultipleForegroundApps {
                // Simply fade-in a label to inform the user that the camera is unavailable.
            }
        }
    }
    
    @objc // Expose to Objective-C for use with #selector()
    private func sessionInterruptionEnded(notification: NSNotification) {
        
    }
    
    @objc // Expose to Objective-C for use with #selector()
    private func sessionRuntimeError(notification: NSNotification) {
        guard let errorValue = notification.userInfo?[AVCaptureSessionErrorKey] as? NSError else {
            return
        }
        
        let error = AVError(_nsError: errorValue)
        print("Capture session runtime error: \(error)")
        
        /*
        Automatically try to restart the session running if media services were
        reset and the last start running succeeded. Otherwise, enable the user
        to try to resume the session running.
        */
        if error.code == .mediaServicesWereReset {
            sessionQueue.async {
                if self.isSessionRunning {
                    self.session.startRunning()
                    self.isSessionRunning = self.session.isRunning
                } else {
                }
            }
        } else {
        }
    }
    
    @IBAction private func resumeInterruptedSession(_ sender: UIButton) {
        sessionQueue.async {
            /*
            The session might fail to start running. A failure to start the session running will be communicated via
            a session runtime error notification. To avoid repeatedly failing to start the session
            running, we only try to restart the session running in the session runtime error handler
            if we aren't trying to resume the session running.
            */
            self.session.startRunning()
            self.isSessionRunning = self.session.isRunning
            if !self.session.isRunning {
                DispatchQueue.main.async {
                    let message = NSLocalizedString("Unable to resume", comment: "Alert message when unable to resume the session running")
                    let actions = [
                        UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"),
                                      style: .cancel,
                                      handler: nil)]
                    self.alert(title: "foo", message: message, actions: actions)
                }
            } else {
            }
        }
    }
    
    func alert(title: String, message: String, actions: [UIAlertAction]) {
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
    
    // MARK: - Session Cost Check
    
    struct ExceededCaptureSessionCosts: OptionSet {
        let rawValue: Int
        
        static let systemPressureCost = ExceededCaptureSessionCosts(rawValue: 1 << 0)
        static let hardwareCost = ExceededCaptureSessionCosts(rawValue: 1 << 1)
    }
    
    func checkSystemCost() {
        var exceededSessionCosts: ExceededCaptureSessionCosts = []
        
        if session.systemPressureCost > 1.0 {
            exceededSessionCosts.insert(.systemPressureCost)
        }
        
        if session.hardwareCost > 1.0 {
            exceededSessionCosts.insert(.hardwareCost)
        }
        
        switch exceededSessionCosts {
            
        case .systemPressureCost:
            // Choice #1: Reduce front camera resolution
            //if reduceResolutionForCamera(.front) {
            //    checkSystemCost()
            //}
                
            // Choice 2: Reduce the number of video input ports
            //else if reduceVideoInputPorts() {
            //    checkSystemCost()
            //}
                
            // Choice #3: Reduce back camera resolution
            //else if reduceResolutionForCamera(.back) {
            //    checkSystemCost()
            //}
                
            // Choice #4: Reduce front camera frame rate
            if reduceFrameRateForCamera(.front) {
                checkSystemCost()
            }
                
            // Choice #5: Reduce frame rate of back camera
            else if reduceFrameRateForCamera(.back) {
                checkSystemCost()
            } else {
                print("Unable to further reduce session cost.")
            }
            
        case .hardwareCost:
            // Choice #1: Reduce front camera resolution
            if reduceResolutionForCamera(.front) {
                checkSystemCost()
            }
                
            // Choice 2: Reduce back camera resolution
            else if reduceResolutionForCamera(.back) {
                checkSystemCost()
            }
                
            // Choice #3: Reduce front camera frame rate
            else if reduceFrameRateForCamera(.front) {
                checkSystemCost()
            }
                
            // Choice #4: Reduce back camera frame rate
            else if reduceFrameRateForCamera(.back) {
                checkSystemCost()
            } else {
                print("Unable to further reduce session cost.")
            }
            
        case [.systemPressureCost, .hardwareCost]:
            // Choice #1: Reduce front camera resolution
            if reduceResolutionForCamera(.front) {
                checkSystemCost()
            }
                
            // Choice #2: Reduce back camera resolution
            else if reduceResolutionForCamera(.back) {
                checkSystemCost()
            }
                
            // Choice #3: Reduce front camera frame rate
            else if reduceFrameRateForCamera(.front) {
                checkSystemCost()
            }
                
            // Choice #4: Reduce back camera frame rate
            else if reduceFrameRateForCamera(.back) {
                checkSystemCost()
            } else {
                print("Unable to further reduce session cost.")
            }
            
        default:
            break
        }
    }
    
    func reduceResolutionForCamera(_ position: AVCaptureDevice.Position) -> Bool {
        for connection in session.connections {
            for inputPort in connection.inputPorts {
                if inputPort.mediaType == .video && inputPort.sourceDevicePosition == position {
                    guard let videoDeviceInput: AVCaptureDeviceInput = inputPort.input as? AVCaptureDeviceInput else {
                        return false
                    }
                    
                    var dims: CMVideoDimensions
                    
                    var width: Int32
                    var height: Int32
                    var activeWidth: Int32
                    var activeHeight: Int32
                    
                    dims = CMVideoFormatDescriptionGetDimensions(videoDeviceInput.device.activeFormat.formatDescription)
                    activeWidth = dims.width
                    activeHeight = dims.height
                    
                    if ( activeHeight <= 480 ) && ( activeWidth <= 640 ) {
                        return false
                    }
                    
                    let formats = videoDeviceInput.device.formats
                    if let formatIndex = formats.firstIndex(of: videoDeviceInput.device.activeFormat) {
                        
                        for index in (0..<formatIndex).reversed() {
                            let format = videoDeviceInput.device.formats[index]
                            if format.isMultiCamSupported {
                                dims = CMVideoFormatDescriptionGetDimensions(format.formatDescription)
                                width = dims.width
                                height = dims.height
                                
                                if width < activeWidth || height < activeHeight {
                                    do {
                                        try videoDeviceInput.device.lockForConfiguration()
                                        videoDeviceInput.device.activeFormat = format
                                        
                                        videoDeviceInput.device.unlockForConfiguration()
                                        
                                        print("reduced width = \(width), reduced height = \(height)")
                                        
                                        return true
                                    } catch {
                                        print("Could not lock device for configuration: \(error)")
                                        
                                        return false
                                    }
                                    
                                } else {
                                    continue
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return false
    }
    
    func reduceFrameRateForCamera(_ position: AVCaptureDevice.Position) -> Bool {
        for connection in session.connections {
            for inputPort in connection.inputPorts {
                
                if inputPort.mediaType == .video && inputPort.sourceDevicePosition == position {
                    guard let videoDeviceInput: AVCaptureDeviceInput = inputPort.input as? AVCaptureDeviceInput else {
                        return false
                    }
                    let activeMinFrameDuration = videoDeviceInput.device.activeVideoMinFrameDuration
                    var activeMaxFrameRate: Double = Double(activeMinFrameDuration.timescale) / Double(activeMinFrameDuration.value)
                    activeMaxFrameRate -= 10.0
                    
                    // Cap the device frame rate to this new max, never allowing it to go below 15 fps
                    if activeMaxFrameRate >= 15.0 {
                        do {
                            try videoDeviceInput.device.lockForConfiguration()
                            videoDeviceInput.videoMinFrameDurationOverride = CMTimeMake(1, Int32(activeMaxFrameRate))
                            
                            videoDeviceInput.device.unlockForConfiguration()
                            
                            print("reduced fps = \(activeMaxFrameRate)")
                            
                            return true
                        } catch {
                            print("Could not lock device for configuration: \(error)")
                            return false
                        }
                    } else {
                        return false
                    }
                }
            }
        }
        
        return false
    }
    
    func reduceVideoInputPorts () -> Bool {
        var newConnection: AVCaptureConnection
        var result = false
        
        for connection in session.connections {
            for inputPort in connection.inputPorts where inputPort.sourceDeviceType == .builtInDualCamera {
                print("Changing input from dual to single camera")
                
                guard let videoDeviceInput: AVCaptureDeviceInput = inputPort.input as? AVCaptureDeviceInput,
                    let wideCameraPort: AVCaptureInput.Port = videoDeviceInput.ports(for: .video,
                                                                                     sourceDeviceType: .builtInWideAngleCamera,
                                                                                     sourceDevicePosition: videoDeviceInput.device.position).first else {
                                                                                        return false
                }
                
                newConnection = AVCaptureConnection(inputPort: wideCameraPort, videoPreviewLayer: connection.videoPreviewLayer)
                newConnection = AVCaptureConnection(inputPorts: [wideCameraPort], output: connection.output)
                
                session.beginConfiguration()
                
                session.remove(connection)
                
                if session.canAdd(newConnection) {
                    session.add(newConnection)
                    
                    session.commitConfiguration()
                    result = true
                } else {
                    print("Could not add new connection to the session")
                    session.commitConfiguration()
                    return false
                }
            }
        }
        return result
    }
    
    private func setRecommendedFrameRateRangeForPressureState(_ systemPressureState: AVCaptureDevice.SystemPressureState) {
        // The frame rates used here are for demonstrative purposes only for this app.
        // Your frame rate throttling may be different depending on your app's camera configuration.
        let pressureLevel = systemPressureState.level
        if pressureLevel == .serious || pressureLevel == .critical {
            /*if self.movieRecorder == nil || self.movieRecorder?.isRecording == false {
                do {
                    try self.backCameraDeviceInput?.device.lockForConfiguration()
                    
                    print("WARNING: Reached elevated system pressure level: \(pressureLevel). Throttling frame rate.")
                    
                    self.backCameraDeviceInput?.device.activeVideoMinFrameDuration = CMTimeMake(1, 20 )
                    self.backCameraDeviceInput?.device.activeVideoMaxFrameDuration = CMTimeMake(1, 15 )
                    
                    self.backCameraDeviceInput?.device.unlockForConfiguration()
                } catch {
                    print("Could not lock device for configuration: \(error)")
                }
            }*/
        } else if pressureLevel == .shutdown {
            print("Session stopped running due to system pressure level.")
        }
    }
}

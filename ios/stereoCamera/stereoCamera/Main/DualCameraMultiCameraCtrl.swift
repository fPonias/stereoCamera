//
//  DualCameraMultiCameraCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/4/21.
//  Copyright © 2021 cody. All rights reserved.
//

import AVFoundation
import Photos
import MetalKit
import CoreMedia

@available(iOS 13, *)
public class DualCameraMultiCameraCtrl : NSObject, DualCameraController,
                                         AVCaptureVideoDataOutputSampleBufferDelegate,
                                         AVCaptureAudioDataOutputSampleBufferDelegate{
    let dualCameraCtrl:DualCameraCtrl
    
    init(dualCameraCtrl:DualCameraCtrl) {
        self.dualCameraCtrl = dualCameraCtrl
    }
    
    private let session = AVCaptureMultiCamSession()
    private var isSessionRunning = false
    private let dataOutputQueue = DispatchQueue(label: "data output queue")
    var setupResult: DualCameraCtrl.SessionSetupResult = .success
    
    var leftCameraStr: CameraStr?
    var rightCameraStr: CameraStr?
    var audioStr: AudioStr?
    
    var verticalOffset:CGFloat = Cookie.instance.verticalOffset
    
    func getZoomSide() -> ImageProcessor.Side {
        guard let leftCameraStr = leftCameraStr,
              let rightCameraStr = rightCameraStr,
              let leftFOV = leftCameraStr.fieldOfView,
              let rightFOV = rightCameraStr.fieldOfView
         else {
             return .LEFT
        }

        return (leftFOV > rightFOV) ? .LEFT : .RIGHT
    }
    
    
    private func cleanUp()
    {
        dualCameraCtrl.sessionQueue.async {
            if self.setupResult == .success {
                self.session.stopRunning()
                self.isSessionRunning = self.session.isRunning
                self.removeObservers()
            }
        }
        
    }
    
    
    private var sessionRunningContext = 0
    private var keyValueObservations = [NSKeyValueObservation]()
    
    private func addObservers()
    {
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
            dualCameraCtrl.sessionQueue.async {
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
        dualCameraCtrl.sessionQueue.async {
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
                    self.dualCameraCtrl.alert(title: "foo", message: message, actions: actions)
                }
            } else {
            }
        }
    }
    
    public func viewWillAppear()
    {
        dualCameraCtrl.sessionQueue.async { [weak self] in
            switch self?.setupResult {
            case .success:
                // Only setup observers and start the session running if setup succeeded.
                self?.addObservers()
                self?.session.startRunning()
                self?.isSessionRunning = self?.session.isRunning ?? false
                
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
                    
                    self?.dualCameraCtrl.present(alertController, animated: true, completion: nil)
                }
                
            case .configurationFailed:
                DispatchQueue.main.async {
                    let alertMsg = "Alert message when something goes wrong during capture session configuration"
                    let message = NSLocalizedString("acapture media", comment: alertMsg)
                    let alertController = UIAlertController(title: "foo", message: message, preferredStyle: .alert)
                    
                    alertController.addAction(UIAlertAction(title: NSLocalizedString("OK", comment: "Alert OK button"),
                                                            style: .cancel,
                                                            handler: nil))
                    
                    self?.dualCameraCtrl.present(alertController, animated: true, completion: nil)
                }
                
            case .multiCamNotSupported:
                DispatchQueue.main.async {
                    let alertMessage = "Alert message when multi cam is not supported"
                    let message = NSLocalizedString("Multi Cam Not Supported", comment: alertMessage)
                    let alertController = UIAlertController(title: "foo", message: message, preferredStyle: .alert)
                    
                    self?.dualCameraCtrl.present(alertController, animated: true, completion: nil)
                }
            case .none:
                _ = 0
            }
        }
    }
    
    public func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        if output is AVCaptureVideoDataOutput {
            
            
            if (connection == leftCameraStr?.videoDataOutputConnection) {
                let zoom = getZoomSide() == .LEFT ? _zoom : 1.0
                dualCameraCtrl.captureOutput(didOutput: sampleBuffer, isLeft: true, zoom: zoom, offset: CGPoint())
            } else {
                let zoom = getZoomSide() == .RIGHT ? _zoom : 1.0
                dualCameraCtrl.captureOutput(didOutput: sampleBuffer, isLeft: false, zoom: zoom, offset: _offset)
            }
        } else if output is AVCaptureAudioDataOutput {
            dualCameraCtrl.captureOutput(audioOutput: sampleBuffer)
        }
        
        if (shutterWaiting) {
            if (connection == leftCameraStr?.videoDataOutputConnection) {
                shutterLeft = sampleBuffer
            } else {
                shutterRight = sampleBuffer
            }
            
            if (shutterLeft != nil && shutterRight != nil) {
                getSyncedFrames2()
            }
        }
    }
      
    
    private var _zoom:Float = 1.0
    public func getZoom() -> Float { return _zoom }
    public func setZoom(_ zoom:Float) { _zoom = zoom }
    
    private var _offset = CGPoint()
    public func getOffset() -> CGPoint { return _offset }
    public func setOffset(_ offset: CGPoint) { _offset = offset }
    
    // Must be called on the session queue
    func configureSession() -> Bool {
        guard setupResult == .success else { return true }
        
        guard AVCaptureMultiCamSession.isMultiCamSupported else {
            print("MultiCam not supported on this device")
            setupResult = .multiCamNotSupported
            return false
        }
        
        // When using AVCaptureMultiCamSession, it is best to manually add connections from AVCaptureInputs to AVCaptureOutputs
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
            if setupResult == .success {
                checkSystemCost()
            }
        }
        
        return true
    }
    
    func setCameraPair(pair: DualCameraCtrl.CameraPair) {
        if (rightCameraStr != nil) {
            setZoom(1.0)
            setZoom(1.0)

            session.stopRunning()
            session.beginConfiguration()
            
            for conn in session.connections {
                session.remove(conn)
            }
            
            for input in session.inputs {
                session.removeInput(input)
            }
            
            for output in session.outputs {
                session.removeOutput(output)
            }
            session.commitConfiguration()
        }
        
        rightCameraStr = configureCamera(pair.right)
        leftCameraStr = configureCamera(pair.left)
        guard rightCameraStr != nil && leftCameraStr != nil else {
            setupResult = .configurationFailed
            return
        }
        
        let cookieZoom = Cookie.instance.zoom
        let zoom:Float
        if (cookieZoom == nil) {
            zoom = calculateZoom(leftCameraStr: leftCameraStr!, rightCameraStr: rightCameraStr!)
            Cookie.instance.zoom = zoom
        } else {
            zoom = cookieZoom!
        }
        
        setZoom(zoom)
        
        setOffset(CGPoint(x: 0.0, y: verticalOffset))
        
        
        audioStr = configureAudio()
        guard audioStr != nil else {
            setupResult = .configurationFailed
            return
        }
        
        
        session.startRunning()
    }
    
    private func calculateZoom(leftCameraStr:CameraStr, rightCameraStr:CameraStr) -> Float {
        let rightFov = rightCameraStr.fieldOfView ?? 0.0
        let rightFovRad = rightFov * Float.pi / 180.0
        let rightWidth = 2.0 * tan(rightFovRad / 2.0)
        
        let leftFov = leftCameraStr.fieldOfView ?? 0.0
        let leftFovRad = leftFov * Float.pi / 180.0
        let leftWidth = 2.0 * tan(leftFovRad / 2.0)
        
        var zoom = rightWidth / leftWidth
        
        if (zoom < 1.0) { zoom = 1.0 / zoom }
        
        return zoom
    }
    
    struct CameraStr {
        var device:AVCaptureDevice?
        var deviceInput:AVCaptureDeviceInput?
        var videoDataOutput:AVCaptureVideoDataOutput?
        var videoDataOutputConnection:AVCaptureConnection?
        var photoOutput:AVCapturePhotoOutput?
        var fieldOfView:Float?
    }
    
    private func configureCamera(_ device:AVCaptureDevice, maxDim:Int32 = Int32.max) -> CameraStr? {
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
        }
        
        let deviceInput:AVCaptureDeviceInput
        let videoDataOutput = AVCaptureVideoDataOutput()
        
        // Add the back camera input to the session
        do {
            deviceInput = try AVCaptureDeviceInput(device: device)
            
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
            }
        }
        
        sorted.sort { (a, b) -> Bool in
            return a.formatDescription.dimensions.height > b.formatDescription.dimensions.height
        }
        
        let attempt = 0
        
        do {
            try device.lockForConfiguration()
            device.activeFormat = sorted[attempt]
            let fr = sorted[attempt].videoSupportedFrameRateRanges
            let videoMinFrameDurationOverride = CMTimeMake(1, Int32(30))
            device.activeVideoMaxFrameDuration = fr[0].maxFrameDuration
            device.activeVideoMinFrameDuration = videoMinFrameDurationOverride
            
            if (device.isFocusModeSupported(.continuousAutoFocus)) {
                device.focusMode = .continuousAutoFocus
            }
            
            device.unlockForConfiguration()
        } catch {
            print ("Could not adjust camera resolution")
            return nil
        }
        
        let fmt = sorted[attempt]
        let hiRes = fmt.highResolutionStillImageDimensions
        let preRes = fmt.formatDescription.dimensions
        let fov = fmt.videoFieldOfView
        
        print ("fov \(fov)")
        
        print ("set camera resoulution to hi", hiRes.width, "x", hiRes.height, "lo", preRes.width, "x", preRes.height, separator: " ")
        
        // Find the back camera device input's video port
        guard let backCameraVideoPort = deviceInput.ports(for: .video,
                  sourceDeviceType: device.deviceType,
                  sourceDevicePosition: device.position).first
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
        
        return CameraStr(device: device, deviceInput: deviceInput, videoDataOutput: videoDataOutput, videoDataOutputConnection: videoDataOutputConnection, photoOutput: nil, fieldOfView: fov)
    }
    
    struct AudioStr {
        var input:AVCaptureDeviceInput?
        var output:AVCaptureAudioDataOutput?
        var connection:AVCaptureConnection?
    }
    
    func configureAudio() -> AudioStr? {
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
        }
        
        // Find the microphone
        guard let microphone = AVCaptureDevice.default(for: .audio) else {
            print("Could not find the microphone")
            return nil
        }
        
        // Add the microphone input to the session
        let microphoneDeviceInput:AVCaptureDeviceInput
        do {
            microphoneDeviceInput = try AVCaptureDeviceInput(device: microphone)
            
            guard session.canAddInput(microphoneDeviceInput) else {
                    print("Could not add microphone device input")
                    return nil
            }
            session.addInputWithNoConnections(microphoneDeviceInput)
        } catch {
            print("Could not create microphone input: \(error)")
            return nil
        }
        
        // Find the audio device input's back audio port
        guard let backMicrophonePort = microphoneDeviceInput.ports(for: .audio,
             sourceDeviceType: microphone.deviceType,
             sourceDevicePosition: .back).first
        else {
                print("Could not find the back camera device input's audio port")
                return nil
        }
        
        // Add the back microphone audio data output
        let backMicrophoneAudioDataOutput = AVCaptureAudioDataOutput()
        guard session.canAddOutput(backMicrophoneAudioDataOutput)
        else {
            print("Could not add the back microphone audio data output")
            return nil
        }
        session.addOutputWithNoConnections(backMicrophoneAudioDataOutput)
        backMicrophoneAudioDataOutput.setSampleBufferDelegate(self, queue: dataOutputQueue)
        
        // Connect the back microphone to the back audio data output
        let backMicrophoneAudioDataOutputConnection = AVCaptureConnection(inputPorts: [backMicrophonePort], output: backMicrophoneAudioDataOutput)
        guard session.canAdd(backMicrophoneAudioDataOutputConnection)
        else {
            print("Could not add a connection to the back microphone audio data output")
            return nil
        }
        session.add(backMicrophoneAudioDataOutputConnection)
        
        return AudioStr(input: microphoneDeviceInput, output: backMicrophoneAudioDataOutput, connection: backMicrophoneAudioDataOutputConnection)
    }
    
    func getVideoSettings() -> [String : Any]? {
        guard let videoStr = leftCameraStr,
              let videoOut = videoStr.videoDataOutput
        else { return nil }
        
        let videoSettings = videoOut.recommendedVideoSettingsForAssetWriter(writingTo: .mov) as? [String: NSObject]
        
        if videoSettings == nil {
            print ("Could not get video settings")
            return nil
        }
        
        return videoSettings
    }
    
    func getAudioSettings() -> [String : Any]? {
        guard let audioStr = audioStr,
              let audioOut = audioStr.output
        else { return nil }
        
        let backMicrophoneAudioSettings = audioOut.recommendedAudioSettingsForAssetWriter(writingTo: .mov) as? [String: NSObject]
        
        if backMicrophoneAudioSettings == nil {
            print("Could not get back microphone audio settings")
            return nil
        }
        
        return backMicrophoneAudioSettings
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
    
    private var shutterProcessing = false
    private var shutterWaiting = false
    private var shutterLeft:CMSampleBuffer?
    private var shutterRight:CMSampleBuffer?
    private var shutterCallback:((_ left:CVPixelBuffer, _ right:CVPixelBuffer) -> Void)?
    
    func getSyncedFrames(callback: @escaping(CVPixelBuffer, CVPixelBuffer) -> Void)
    {
        if (shutterWaiting) {
            return
        }
        
        shutterLeft = nil
        shutterRight = nil
        shutterWaiting = true
        shutterCallback = callback
    }
    
    private func getSyncedFrames2()
    {
        guard let ls = shutterLeft,
              let rs = shutterRight,
              let lPixelBuffer = CMSampleBufferGetImageBuffer(ls),
              let rPixelBuffer = CMSampleBufferGetImageBuffer(rs)
        else { return }
        
        AudioServicesPlaySystemSound(1108)
        shutterCallback?(lPixelBuffer, rPixelBuffer)
        shutterWaiting = false
    }
    
    func sliderChanged(value: Float, target: AdjustmentItem) {
        if (target.type == .VERTICAL_OFFSET) {
            verticalOffset = CGFloat(value)
            setOffset(CGPoint(x: 0.0, y: verticalOffset))
        } else if (target.type == .ZOOM) {
            setZoom(value)
        }
    }
}

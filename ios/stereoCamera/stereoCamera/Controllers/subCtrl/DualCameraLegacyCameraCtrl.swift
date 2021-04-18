//
//  DualCameraLegacyCameraCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/4/21.
//  Copyright © 2021 cody. All rights reserved.
//

import AVFoundation
import Photos
import MetalKit
import CoreMedia

@available(iOS 13.0, *)
class DualCameraLegacyCameraCtrl : NSObject, DualCameraController,
                                   AVCaptureVideoDataOutputSampleBufferDelegate,
                                   AVCapturePhotoCaptureDelegate {
    private let dualCameraCtrl:DualCameraCtrl
    var setupResult: DualCameraCtrl.SessionSetupResult = .success
    private let dataOutputQueue = DispatchQueue(label: "data output queue")
    var leftCameraStr: CameraStr?
    var rightCameraStr: CameraStr?
    
    init(dualCameraCtrl:DualCameraCtrl){
        self.dualCameraCtrl = dualCameraCtrl
    }
    
    func viewWillAppear() {
        dualCameraCtrl.sessionQueue.async { [weak self] in
            switch self?.setupResult {
            case .success:
                // Only setup observers and start the session running if setup succeeded.
                //self?.addObservers()
                self?.leftCameraStr?.session?.startRunning()
                //self?.isSessionRunning = self?.session.isRunning ?? false
            case .none:
                let _ = 0
            case .some(.notAuthorized):
                let _ = 0
            case .some(.configurationFailed):
                let _ = 0
            case .some(.multiCamNotSupported):
                let _ = 0
            }
        }
    }
        
    func getZoom() -> Float {
        let z = rightCameraStr?.deviceInput?.device.videoZoomFactor
        if z != nil { return Float(z!) } else { return 1.8 }
    }

    func setZoom(_ zoom:Float) {
        do {
            try rightCameraStr?.deviceInput?.device.lockForConfiguration()
            rightCameraStr?.deviceInput?.device.videoZoomFactor = CGFloat(zoom)
            rightCameraStr?.deviceInput?.device.unlockForConfiguration()
        } catch {}
        
    }
    
    // Must be called on the session queue
    func configureSession() -> Bool {
        guard setupResult == .success else { return false }
         
        
        leftCameraStr = configureCamera(.builtInTelephotoCamera, maxDim: 1600)
        guard leftCameraStr != nil else {
            setupResult = .configurationFailed
            return false
        }
        
        let leftDim = leftCameraStr?.device?.activeFormat.highResolutionStillImageDimensions.height ?? 1024
        let maxDim = Int32(Float(leftDim) * 0.8)
        rightCameraStr = configureCamera(.builtInWideAngleCamera, maxDim: maxDim)
        guard rightCameraStr != nil else {
            setupResult = .configurationFailed
            return false
        }
        setZoom(1.8)
        
        return true
    }
    
    struct CameraStr {
        var session:AVCaptureSession?
        var device:AVCaptureDevice?
        var deviceInput:AVCaptureDeviceInput?
        var videoDataOutput:AVCaptureVideoDataOutput?
        var videoDataOutputConnection:AVCaptureConnection?
        var photoOutput:AVCapturePhotoOutput?
    }
    
    private func configureCamera(_ type:AVCaptureDevice.DeviceType, maxDim:Int32 = Int32.max) -> CameraStr? {
        let session = AVCaptureSession()
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
        }
        
        session.sessionPreset = .photo
        
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
            if fmt.formatDescription.dimensions.height < maxDim {
                sorted.append(fmt)
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
        
        let videoDataOutputConnection:AVCaptureConnection?
        // Add the back camera video data output
        guard session.canAddOutput(videoDataOutput) else {
            print("Could not add the back camera video data output")
            return nil
        }
        session.addOutputWithNoConnections(videoDataOutput)
        videoDataOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        videoDataOutput.setSampleBufferDelegate(self, queue: dataOutputQueue)
        
        // Connect the back camera device input to the back camera video data output
        videoDataOutputConnection = AVCaptureConnection(inputPorts: [backCameraVideoPort], output: videoDataOutput)
        guard session.canAdd(videoDataOutputConnection!) else {
            print("Could not add a connection to the back camera video data output")
            return nil
        }
        session.add(videoDataOutputConnection!)
        
        let stillOutput = AVCapturePhotoOutput()
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
        session.add(stillDataOutputConnection)
        
        return CameraStr(session: session, device: backCamera, deviceInput: deviceInput, videoDataOutput: videoDataOutput, videoDataOutputConnection: videoDataOutputConnection, photoOutput: stillOutput)
    }
    
    private var shutterProcessing = false
    private var shutterWaiting = false
    private var shutterLeft:CVPixelBuffer?
    private var shutterRight:CVPixelBuffer?
    private var shutterCallback:((_ left:CVPixelBuffer, _ right:CVPixelBuffer) -> Void)?
    let histogram = Histogram()
    
    //frames aren't syncable on early dual camera iphone models:
    //the current algorithm triggers a photo on the first currently running
    //preview camera.  Once the photo is captured, the left camera session is stopped
    //and the other is started.  Camera frames are initially black until they "power up"
    //so we compensate by reading the histogram
    //calculated in captureOutput() until the histogram average is close to the average
    //of the first captured frame (usually around 15 frames - 1/4 second).
    //Delays seem to be large fractions of a single second
    //a dual phone setup has less delay but have obvious alignment and additional sync issues
    func getSyncedFrames(callback: @escaping(CVPixelBuffer, CVPixelBuffer) -> Void)
    {
        if (shutterWaiting) {
            return
        }
        
        shutterLeft = nil
        shutterRight = nil
        rightCount = 0
        shutterWaiting = true
        shutterCallback = callback
        
        //let lsettings = AVCapturePhotoSettings(format: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA])
        //leftCameraStr?.photoOutput?.capturePhoto(with: lsettings, delegate: self)
    }
    
    func photoOutput(_ output: AVCapturePhotoOutput, didCapturePhotoFor resolvedSettings: AVCaptureResolvedPhotoSettings) {
        if (output == leftCameraStr?.photoOutput) {
            leftCameraStr?.session?.stopRunning()
            rightCameraStr?.session?.startRunning()
        } else {
            rightCameraStr?.session?.stopRunning()
            leftCameraStr?.session?.startRunning()
        }
    }
    
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if (output == leftCameraStr?.photoOutput) {
            shutterLeft = photo.pixelBuffer
            
            leftCameraStr?.session?.stopRunning()
            rightCameraStr?.session?.startRunning()
            
            rightCount = 0
        } else {
            shutterRight = photo.pixelBuffer
            
            guard let sl = shutterLeft,
                  let sr = shutterRight
            else { return }
            
            shutterCallback?(sl, sr)
            shutterWaiting = false
        }
    }
    
    private var rightCount = 0
    private var leftAverage:Float = 0.0
    
    public func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        
        if (connection == leftCameraStr?.videoDataOutputConnection) {
            dualCameraCtrl.captureOutput(didOutput: sampleBuffer, isLeft: true)
            
            if shutterWaiting {
                
                if let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
                    shutterLeft = pixelBuffer
                    histogram.setPixels(pixels:pixelBuffer)
                    histogram.setZoom(1.0)
                    leftAverage = histogram.average()
                }
                
                leftCameraStr?.session?.stopRunning()
                rightCameraStr?.session?.startRunning()
            }
        } else {
            
            if (shutterWaiting) {
                dualCameraCtrl.captureOutput(didOutput: sampleBuffer, isLeft: true)
                
                rightCount += 1
                guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
                histogram.setPixels(pixels:pixelBuffer)
                histogram.setZoom(1.0)
                let avg = histogram.average()
                
                let diff = abs(avg - leftAverage)
                print ("diff " + String(diff))
                if ((rightCount > 10 && diff < 10.0) || rightCount >= 60) {
                    shutterRight = pixelBuffer
                    rightCameraStr?.session?.stopRunning()
                    leftCameraStr?.session?.startRunning()
                }
            }
        }
        
        if (shutterWaiting && shutterRight != nil && shutterLeft != nil) {
            AudioServicesPlaySystemSound(1108)
            shutterCallback?(shutterLeft!, shutterRight!)
            shutterWaiting = false
        }
    }
}

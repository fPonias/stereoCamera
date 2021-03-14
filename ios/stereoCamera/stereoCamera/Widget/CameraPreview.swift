//
//  CameraPreview.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/17/18.
//  Copyright © 2018 cody. All rights reserved.
//

import AVFoundation
import GLKit

class CameraPreview : GLKView, AVCaptureVideoDataOutputSampleBufferDelegate, AVCapturePhotoCaptureDelegate
{
    override init(frame: CGRect)
    {
        super.init(frame: frame)
        
        updateTransform()
        setupOpenGL()
    }
    
    override init(frame: CGRect, context: EAGLContext)
    {
        super.init(frame: frame, context: context)
        
        updateTransform()
        setupOpenGL()
    }
    
    required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)

        updateTransform()
        setupOpenGL()
    }
    
    private func setupOpenGL()
    {
        captureSession = AVCaptureSession()
        glContext = EAGLContext(api: .openGLES2)!
        ciContext = CIContext(eaglContext: glContext)
        context = glContext
    }
    
    private var _zoom:Float = 1.0
    var zoom:Float
    {
        get {return _zoom}
        set
        {
            _zoom = newValue
            updateTransform()
        }
    }
    
    func startCamera(cameraPosition: AVCaptureDevice.Position = .unspecified, quality: ImageQuality = ImageQuality.LOW)
    {
        switch AVCaptureDevice.authorizationStatus(for: .video)
        {
        case .authorized:
            startCamera2(cameraPosition: cameraPosition, quality: quality)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                if (granted)
                {
                    self.startCamera2(cameraPosition: cameraPosition, quality: quality)
                }
            }
        case .denied:
            return
        case .restricted:
            return
        }
    }
    
    private var captureSession:AVCaptureSession!
    private var glContext:EAGLContext!
    private var ciContext:CIContext!
    private var photoOutput = AVCapturePhotoOutput()
    private var _currentCamera:AVCaptureDevice? = nil
    private var _imageQuality:ImageQuality = .LOW
    
    var currentCamera:AVCaptureDevice?
    {
        get { return _currentCamera }
    }
    
    private func startCamera2(cameraPosition: AVCaptureDevice.Position, quality: ImageQuality)
    {
        _currentCamera = cameraPicker(cameraPosition: cameraPosition)
        _imageQuality = quality
        
        if (_currentCamera == nil)
        {
            backgroundColor = UIColor.black
            return;
        }
        
        captureSession = AVCaptureSession()
        captureSession.beginConfiguration()
        photoOutput = AVCapturePhotoOutput()
        
        guard
            let videoDeviceInput = try? AVCaptureDeviceInput(device: _currentCamera!),
            captureSession.canAddInput(videoDeviceInput)
            else { return }
        captureSession.addInput(videoDeviceInput)
        
        photoOutput.isLivePhotoCaptureEnabled = photoOutput.isLivePhotoCaptureSupported
        guard captureSession.canAddOutput(photoOutput) else {return}
        
        let videoOutput = AVCaptureVideoDataOutput()
        let dq = DispatchQueue(label: "sample buffer delegate")
        videoOutput.setSampleBufferDelegate(self, queue: dq)
        videoOutput.alwaysDiscardsLateVideoFrames = true
        
        guard captureSession.canAddOutput(videoOutput) else {return}
        
        if (quality == .HIGH)
        {
            captureSession.sessionPreset = .photo
        }
        else
        {
            if (captureSession.canSetSessionPreset(.medium)) {
                captureSession.sessionPreset = .medium
            } else if (captureSession.canSetSessionPreset(.low)) {
                captureSession.sessionPreset = .low
            } else {
                captureSession.sessionPreset = .photo
            }
        }
            
        captureSession.addOutput(photoOutput)
        captureSession.addOutput(videoOutput)
        
        
        captureSession.commitConfiguration()
        
        usleep(1000) //getting sync errors where startRunning is called after commitConfiguration
        
        captureSession.startRunning()
    }
    
    func stopCamera()
    {
        //sometimes we have sync issues if the cameras are started and stopped in rapid succession
        usleep(5000)
        
        for input1 in captureSession.inputs
        {
            captureSession.removeInput(input1)
        }
        
        for output1 in captureSession.outputs
        {
            captureSession.removeOutput(output1)
        }
        
        captureSession.stopRunning()
    }
    
    func restartCamera()
    {
        usleep(5000)
        startCamera(cameraPosition: _currentCamera!.position, quality: _imageQuality)
    }
    
    private var frameCount = 0
    
    private var drawPreviews:Bool = true
    
    func setDrawPreviews(_ value:Bool)
    {
        drawPreviews = value
        
        if (drawPreviews == false)
            { stopCamera() }
        else
            { restartCamera() }
    }
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection)
    {
        if (!drawPreviews)
            { return }
        
        if (self.frameCount != 1)
        {
            self.frameCount += 1
            return
        }
        else
            { self.frameCount = 0 }
        
    
        let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)
        var image = CIImage(cvPixelBuffer: pixelBuffer! as CVPixelBuffer, options: nil)
        image = self.rotateScaleImage(image: image)
        
        if self.glContext != EAGLContext.current()
        { EAGLContext.setCurrent(self.glContext) }
        
        self.bindDrawable()
        let extent = image.extent
        let zoomMargin = (extent.width - extent.width / CGFloat(self._zoom)) / CGFloat(2.0)
        //let scale = extent.width / (frame.width * 2.0)
        let from = CGRect(x:extent.origin.x + zoomMargin, y:extent.origin.y + zoomMargin, width: extent.width, height: extent.width)
        
        DispatchQueue.main.async
        { [unowned self, image, from, drawPreviews] in
            let scale = UIScreen.main.scale
            let dest = CGRect(x:0, y:0, width: self.frame.width * scale * CGFloat(self._zoom), height: self.frame.height * scale * CGFloat(self._zoom))
            self.ciContext.draw(image, in:dest, from: from)
            
            if (!drawPreviews)
                { return }
            
            self.display()
        }
    }
    
    private var previewTransform:CGAffineTransform = CGAffineTransform()
    
    public enum CameraOriention:CGFloat
    {
        case DEG_0 = 0.0,
        DEG_90 = 90.0,
        DEG_180 = 180.0,
        DEG_270 = 270.0
    }
    
    public static func orientationToRadians(_ orientation:CameraOriention) -> CGFloat
    {
        switch(orientation)
        {
        case .DEG_0:
            return 0.0
        case .DEG_90:
            return CGFloat(Double.pi)
        case .DEG_180:
            return CGFloat(Double.pi / 2.0)
        case .DEG_270:
            return CGFloat(3 * Double.pi / 2.0)
        }
    }
    
    public static func orientationToByte(_ orientation:CameraOriention) -> UInt8
    {
        switch(orientation)
        {
        case .DEG_0:
            return 0
        case .DEG_90:
            return 1
        case .DEG_180:
            return 2
        case .DEG_270:
            return 3
        }
    }
    
    public static func orientationFromByte(_ b:UInt8) -> CameraOriention
    {
        switch(b)
        {
        case 0:
            return .DEG_0
        case 1:
            return .DEG_90
        case 2:
            return .DEG_180
        case 3:
            return .DEG_270
        default:
            return .DEG_0
        }
    }
    
    public func getScreenOrientation() -> CameraOriention
    {
        let facing = (_currentCamera?.position == AVCaptureDevice.Position.front) ? true : false
        let orient = UIDevice.current.orientation
        switch(orient)
        {
        case .portrait:
            return .DEG_270
        case .portraitUpsideDown:
            return .DEG_90
        case .landscapeRight:
            return (facing) ? .DEG_0 : .DEG_90 //why is this offset by 90 degrees?  I'm so confused.
        case .landscapeLeft:
            return (facing) ? .DEG_90 : .DEG_0
        default:
            return .DEG_0
        }
    }
    
    public func getOrientation() -> CameraOriention
    {
        let facing = (_currentCamera?.position == AVCaptureDevice.Position.front) ? true : false
        let orient = UIDevice.current.orientation
        switch(orient)
        {
        case .portrait:
            return .DEG_90
        case .portraitUpsideDown:
            return .DEG_270
        case .landscapeRight:
            return (facing) ? .DEG_180 : .DEG_0
        case .landscapeLeft:
            return (facing) ? .DEG_0 : .DEG_180
        default:
            return .DEG_0
        }
    }
    
    func updateTransform()
    {
        var orientation = getScreenOrientation();
        var rotation = CameraPreview.orientationToRadians(orientation)
        
        previewTransform = CGAffineTransform(translationX: 0, y: 0)
        
        if (_currentCamera?.position == .front)
            { previewTransform = previewTransform.scaledBy(x: CGFloat(-1.0), y: CGFloat(1.0)) }
        
        if (rotation != 0)
        {
            previewTransform = previewTransform.rotated(by: rotation)
        }
        
        previewTransform = previewTransform.scaledBy(x: CGFloat(_zoom), y: CGFloat(_zoom))
    }
    
    private func rotateScaleImage(image: CIImage) -> CIImage
    {
        var ret:CIImage = image
        
        let inRect = image.extent
        let margin = (inRect.width - inRect.height) / 2
        let cropped = CGRect(x: margin, y: 0, width: inRect.height, height: inRect.height)
        ret = ret.cropped(to: cropped)
        
        ret = ret.transformed(by: previewTransform)
        
        return ret
    }
    
    private func cameraPicker(cameraPosition: AVCaptureDevice.Position) -> AVCaptureDevice?
    {
        let discoverySession = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInDualCamera, .builtInWideAngleCamera, .builtInTelephotoCamera], mediaType: .video, position: cameraPosition)
        let devices = discoverySession.devices
        guard !devices.isEmpty else { print ("No cameras found"); return nil }
        
        return devices[0]
    }
    
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?)
    {
        fireShutterListener?(photo)
        
        fireShutterListener = nil
    }
    
    public typealias FireShutterListener = (_ photo:AVCapturePhoto) -> Void
    
    var fireShutterListener:FireShutterListener?
    
    public func fireShutter(delegate: @escaping FireShutterListener)
    {
        self.fireShutterListener = delegate
        let settings = AVCapturePhotoSettings()
        photoOutput.capturePhoto(with: settings, delegate: self)
    }
}

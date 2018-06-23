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
        setupOpenGL()
    }
    
    override init(frame: CGRect, context: EAGLContext)
    {
        super.init(frame: frame, context: context)
        setupOpenGL()
    }
    
    required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
        setupOpenGL()
    }
    
    func setupOpenGL()
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
        }
    }
    
    func startCamera()
    {
        switch AVCaptureDevice.authorizationStatus(for: .video)
        {
        case .authorized:
            startCamera2()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                if (granted)
                {
                    self.startCamera2()
                }
            }
        case .denied:
            return
        case .restricted:
            return
        }
    }
    
    var captureSession:AVCaptureSession!
    var glContext:EAGLContext!
    var ciContext:CIContext!
    var photoOutput = AVCapturePhotoOutput()
    var _currentCamera:AVCaptureDevice? = nil
    
    public var currentCamera:AVCaptureDevice?
    {
        get { return _currentCamera }
    }
    
    func startCamera2()
    {
        _currentCamera = cameraPicker()
        
        if (_currentCamera == nil)
        {
            backgroundColor = UIColor.black
            return;
        }
        
        captureSession.beginConfiguration()
        
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
        
        guard captureSession.canAddOutput(videoOutput) else {return}
        
        captureSession.sessionPreset = .photo
        captureSession.addOutput(photoOutput)
        captureSession.addOutput(videoOutput)
        
        
        captureSession.commitConfiguration()
        captureSession.startRunning()
    }
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection)
    {
        let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)
        var image = CIImage(cvPixelBuffer: pixelBuffer! as CVPixelBuffer, options: nil)
        image = rotateScaleImage(image: image)
        
        if glContext != EAGLContext.current()
        { EAGLContext.setCurrent(glContext) }
        
        self.bindDrawable()
        let extent = image.extent
        let zoomMargin = (extent.width - extent.width / CGFloat(_zoom)) / CGFloat(2.0)
        let from = CGRect(x:extent.origin.x + zoomMargin, y:extent.origin.y + zoomMargin, width: extent.width, height: extent.width)
        let dest = CGRect(x:0, y:0, width: extent.width, height: extent.height)
        ciContext.draw(image, in:dest, from: from)
        display()
    }
    
    func rotateScaleImage(image: CIImage) -> CIImage
    {
        var ret:CIImage = image
        
        let inRect = image.extent
        let margin = (inRect.width - inRect.height) / 2
        let cropped = CGRect(x: margin, y: 0, width: inRect.height, height: inRect.height)
        ret = ret.cropped(to: cropped)
        
        var rotation:CGFloat = 0.0
        let orient = UIDevice.current.orientation
        switch(orient)
        {
        case .portrait:
            rotation = CGFloat(3 * Double.pi / 2.0)
        case .portraitUpsideDown:
            rotation = CGFloat(Double.pi / 2.0)
        case .landscapeRight:
            rotation = CGFloat(Double.pi)
        default:
            rotation = 0.0
        }
        
        var trans = CGAffineTransform(translationX: 0, y: 0)
        
        if (rotation != 0)
        {
            trans = trans.rotated(by: rotation)
        }
        
        trans = trans.scaledBy(x: CGFloat(_zoom), y: CGFloat(_zoom))
        ret = ret.transformed(by: trans)
        
        return ret
    }
    
    func cameraPicker() -> AVCaptureDevice?
    {
        let discoverySession = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInDualCamera, .builtInWideAngleCamera, .builtInTelephotoCamera], mediaType: .video, position: .unspecified)
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

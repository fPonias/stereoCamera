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
    
    func startCamera(cameraPosition: AVCaptureDevice.Position = .unspecified)
    {
        switch AVCaptureDevice.authorizationStatus(for: .video)
        {
        case .authorized:
            startCamera2(cameraPosition: cameraPosition)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                if (granted)
                {
                    self.startCamera2(cameraPosition: cameraPosition)
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
    
    var currentCamera:AVCaptureDevice?
    {
        get { return _currentCamera }
    }
    
    private func startCamera2(cameraPosition: AVCaptureDevice.Position)
    {
        _currentCamera = cameraPicker(cameraPosition: cameraPosition)
        
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
        
        captureSession.sessionPreset = .photo
        captureSession.addOutput(photoOutput)
        captureSession.addOutput(videoOutput)
        
        
        captureSession.commitConfiguration()
        captureSession.startRunning()
    }
    
    func stopCamera()
    {
        captureSession.stopRunning()
    }
    
    private var frameCount = 0
    
    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection)
    {
        if (frameCount != 1)
        {
            frameCount += 1
            return
        }
        else
            { frameCount = 0 }
        
    
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
    
    private var previewTransform:CGAffineTransform = CGAffineTransform()
    
    func updateTransform()
    {
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

enum CameraPreviewOverlayType:Int
{
    case None,
    Crosshairs,
    Thirds,
    Fourths,
    Ghost
};

//
//  CameraPreview.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/17/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation
import GLKit

class CameraPreview : VideoPreview
{
    func startCamera(cameraPosition: AVCaptureDevice.Position = .unspecified, quality: ImageQuality = ImageQuality.LOW)
    {
        captureSession = AVCaptureSession()
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
    {/*
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
        }*/
    }
    
    /*
    override func isFacing() -> Bool
    {
        return (_currentCamera?.position == AVCaptureDevice.Position.front) ? true : false
    }*/

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

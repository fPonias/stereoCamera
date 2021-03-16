//
//  VideoPreview.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/14/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation
import MetalKit

class VideoPreview : MTKView, AVCaptureVideoDataOutputSampleBufferDelegate, AVCapturePhotoCaptureDelegate {
    
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
    
    open func isFacing() -> Bool
    {
        return false
    }
    
    public func getScreenOrientation() -> CameraOriention
    {
        let facing = isFacing()
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
        let facing = isFacing()
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
        let orientation = getScreenOrientation();
        let rotation = CameraPreview.orientationToRadians(orientation)
        
        previewTransform = CGAffineTransform(translationX: 0, y: 0)
        
        if (isFacing())
            { previewTransform = previewTransform.scaledBy(x: CGFloat(-1.0), y: CGFloat(1.0)) }
        
        if (rotation != 0)
        {
            previewTransform = previewTransform.rotated(by: rotation)
        }
        
        previewTransform = previewTransform.scaledBy(x: CGFloat(_zoom), y: CGFloat(_zoom))
    }
    
    func rotateScaleImage(image: CIImage) -> CIImage
    {
        var ret:CIImage = image
        
        let inRect = image.extent
        let margin = (inRect.width - inRect.height) / 2
        let cropped = CGRect(x: margin, y: 0, width: inRect.height, height: inRect.height)
        ret = ret.cropped(to: cropped)
        
        ret = ret.transformed(by: previewTransform)
        
        return ret
    }
    
    func captureOutput(_ output: AVCaptureOutput, didDrop sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        print("frame dropped")
    }
    
    func captureOutput(captureOutput: AVCaptureOutput!, didOutputSampleBuffer sampleBuffer:CMSampleBuffer!, fromConnection connection: AVCaptureConnection!)
    {
        print("handling frame")
        let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer)
        let image = CIImage(cvPixelBuffer: pixelBuffer!)
        let transformImage = image
        
        let newFrame = CGRect(x:0, y:0, width: frame.width, height: frame.height)
        
        /*if glContext != EAGLContext.current() {
            EAGLContext.setCurrent(glContext)
        }
    
        bindDrawable()
        ciContext.draw(transformImage, in: newFrame, from: transformImage.extent)
        display()*/
    }
    
}

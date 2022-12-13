//
//  DualCameraFakeCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 11/6/21.
//  Copyright © 2021 cody. All rights reserved.
//

import AVFoundation
import Photos
import MetalKit
import CoreMedia

public class DualCameraFakeCtrl : DualCameraController {
    private let greyImg:UIImage?
    private var pixelBuf:CVPixelBuffer?
    private let target:DualCameraCtrl
    
    init(target:DualCameraCtrl) {
        self.target = target
        pixelBuf = nil
        greyImg = UIImage.init(named: "grey")
        
        guard let greyImg = greyImg else { return }
        pixelBuf = DualCameraFakeCtrl.buffer(from: greyImg)
    }
    
    func getZoomSide() -> ImageProcessor.Side {
        return .RIGHT
    }
    
    static func buffer(from image: UIImage) -> CVPixelBuffer? {
        let attrs = [kCVPixelBufferCGImageCompatibilityKey: kCFBooleanTrue, kCVPixelBufferCGBitmapContextCompatibilityKey: kCFBooleanTrue] as CFDictionary
        var pixelBuffer : CVPixelBuffer?
        let status = CVPixelBufferCreate(kCFAllocatorDefault, Int(image.size.width), Int(image.size.height), kCVPixelFormatType_32ARGB, attrs, &pixelBuffer)
        guard (status == kCVReturnSuccess) else {
            return nil
        }
        
        CVPixelBufferLockBaseAddress(pixelBuffer!, CVPixelBufferLockFlags(rawValue: 0))
        let pixelData = CVPixelBufferGetBaseAddress(pixelBuffer!)
        
        let rgbColorSpace = CGColorSpaceCreateDeviceRGB()
        let context = CGContext(data: pixelData, width: Int(image.size.width), height: Int(image.size.height), bitsPerComponent: 8, bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer!), space: rgbColorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)
        
        context?.translateBy(x: 0, y: image.size.height)
        context?.scaleBy(x: 1.0, y: -1.0)
        
        UIGraphicsPushContext(context!)
        image.draw(in: CGRect(x: 0, y: 0, width: image.size.width, height: image.size.height))
        UIGraphicsPopContext()
        CVPixelBufferUnlockBaseAddress(pixelBuffer!, CVPixelBufferLockFlags(rawValue: 0))
        
        return pixelBuffer
    }
    
    func getZoom() -> Float {
        return 1.0
    }
    
    func setZoom(_ zoom: Float) {}
    
    func getOffset() -> CGPoint {
        return CGPoint()
    }
    
    func setOffset(_ offset: CGPoint) {}
    
    func configureSession() -> Bool {
        return true
    }
    
    func setCameraPair(pair: DualCameraCtrl.CameraPair) {}
    
    func viewWillAppear() {
        guard let pixelBuf = pixelBuf else { return }
        target.captureOutput(staticOutput: pixelBuf, isLeft: true, zoom: 1.0, offset: CGPoint())
        target.captureOutput(staticOutput: pixelBuf, isLeft: false, zoom: 1.0, offset: CGPoint())
    }
    
    func getSyncedFrames(callback: @escaping (CVPixelBuffer, CVPixelBuffer) -> Void) {
        guard let pixelBuf = pixelBuf else { return }
        callback(pixelBuf, pixelBuf)
    }
    
    func getAudioSettings() -> [String : Any]? {
        return nil
    }
    
    func getVideoSettings() -> [String : Any]? {
        return nil
    }
    
    func sliderChanged(value: Float, target: AdjustmentItem) {}
    
    
}

//
//  ImageUtils.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/23/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage
import UIKit

public class ImageUtils
{
    struct Margin {
        var left:Int
        var top:Int
        var right:Int
        var bottom:Int
        var width:Int
        var height:Int
    }
    
    struct MarginFloat {
        var left:Float
        var top:Float
        var right:Float
        var bottom:Float
        var widthPitch:Float
        var heightPitch:Float
    }
    
    static func marginFloatToMtlArr(margins:MarginFloat, ptr:UnsafeMutablePointer<Float32>) {
        (ptr + 0).pointee = margins.left
        (ptr + 1).pointee = margins.top
        (ptr + 2).pointee = margins.right
        (ptr + 3).pointee = margins.bottom
        (ptr + 4).pointee = margins.widthPitch
        (ptr + 5).pointee = margins.heightPitch
    }
    
    struct Size {
        var width:Int
        var height:Int
    }
    
    static func findFloatMargins(size:Size, zoom:Float, offset:CGPoint) -> MarginFloat
    {
        let margins = findMargins(size: size, zoom: zoom, offset: offset)
        var ret = MarginFloat(left: 0, top: 0, right: 0, bottom: 0, widthPitch: 0, heightPitch: 0)
        
        ret.left = Float(margins.left) / Float(size.width)
        ret.top = Float(margins.top) / Float(size.height)
        ret.right = (Float(size.width) - Float(margins.right)) / Float(size.width)
        ret.bottom = (Float(size.height) - Float(margins.bottom)) / Float(size.height)
        ret.widthPitch = (ret.right - ret.left) / Float(margins.width)
        ret.heightPitch = (ret.bottom - ret.top) / Float(margins.height)

        return ret
    }
    
    static func findMargins(size:Size, zoom:Float, offset:CGPoint) -> Margin
    {
        var ret = Margin(left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0)
        
        let padding = 1.0 - (1.0 / zoom)
        if (size.width < size.height)
        {
            let margin = (size.height - size.width) / 2
            let wPadding = (padding * Float(size.width)) / 2.0
            let wPaddingPx = Int(wPadding)
            let hPaddingPx = Int(wPadding) + margin
        
            ret.left = wPaddingPx
            ret.right = wPaddingPx
            ret.top = hPaddingPx
            ret.bottom = hPaddingPx
            ret.width = size.width - 2 * wPaddingPx
            ret.height = size.height - 2 * hPaddingPx
        }
        else
        {
            let margin = (size.width - size.height) / 2
            let hPadding = (padding * Float(size.height)) / 2.0
            let hPaddingPx = Int(hPadding)
            let wPaddingPx = Int(hPadding) + margin
        
            ret.left = wPaddingPx
            ret.right = wPaddingPx
            ret.top = hPaddingPx
            ret.bottom = hPaddingPx
            ret.width = size.width - 2 * wPaddingPx
            ret.height = size.height - 2 * hPaddingPx
        }
        
        if (offset.x != 0) {
            ret.left += Int(offset.x)
            ret.right -= Int(offset.x)
        }
        
        if (offset.y != 0) {
            ret.top += Int(offset.y)
            ret.bottom -= Int(offset.y)
        }
        
        return ret
    }
    
    static func copyBuffer(base:CVImageBuffer) -> CVPixelBuffer?
    {
        CVPixelBufferLockBaseAddress(base, CVPixelBufferLockFlags())
        let width = CVPixelBufferGetWidth(base)
        let height = CVPixelBufferGetHeight(base)
        let widthSz = CVPixelBufferGetBytesPerRow(base)
        let ptr = CVPixelBufferGetBaseAddress(base)
        
        var ret:CVPixelBuffer?
        let status = CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32BGRA, [kCVPixelBufferMetalCompatibilityKey: true] as CFDictionary, &ret)
        guard ret != nil else { return nil }
        CVPixelBufferLockBaseAddress(ret!, CVPixelBufferLockFlags())
        let retPtr = CVPixelBufferGetBaseAddress(ret!)
        
        memcpy(retPtr, ptr, height * widthSz)
        CVPixelBufferUnlockBaseAddress(base, CVPixelBufferLockFlags())
        CVPixelBufferUnlockBaseAddress(ret!, CVPixelBufferLockFlags())
        
        return ret
    }
    
    public enum CameraOrientation:CGFloat
    {
        case DEG_0 = 0.0,
        DEG_90 = 90.0,
        DEG_180 = 180.0,
        DEG_270 = 270.0
    }
    
    public static func orientationToRadians(_ orientation:CameraOrientation) -> CGFloat
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
    
    public static func orientationToByte(_ orientation:CameraOrientation) -> UInt8
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
    
    public static func orientationFromByte(_ b:UInt8) -> CameraOrientation
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
    
    private static func orientationValToProcOrientation(_ value:Int, _ facing:Bool) -> CameraOrientation {
        let facing = false
        switch(value)
        {
        case 1: // portrait
            return .DEG_270
        case 2: // portraitUpsideDown
            return .DEG_90
        case 3: // landscapeRight
            return (facing) ? .DEG_0 : .DEG_0 //why is this offset by 90 degrees?  I'm so confused.
        case 4: // landscapeLeft
            return (facing) ? .DEG_90 : .DEG_90
        default:
            return .DEG_270
        }
    }
    
    public static func deviceOrientationToProcOrientation(_ orientation:UIDeviceOrientation, _ facing:Bool) -> CameraOrientation {
        
        //case unknown = 0
        //case portrait = 1 // Device oriented vertically, home button on the bottom
        //case portraitUpsideDown = 2 // Device oriented vertically, home button on the top
        //case landscapeLeft = 3 // Device oriented horizontally, home button on the right
        //case landscapeRight = 4 // Device oriented horizontally, home button on the left
        //case faceUp = 5 // Device oriented flat, face up
        //case faceDown = 6 // Device oriented flat, face down
        let val = orientation.rawValue
        return orientationValToProcOrientation(val, facing)
    }
    
    public static func imageOrientationToProcOrientation(_ orientation:UIInterfaceOrientation, _ facing:Bool) -> CameraOrientation
    {
        
        //case unknown = 0
        //case portrait = 1
        //case portraitUpsideDown = 2
        //case landscapeLeft = 4
        //case landscapeRight = 3
        var val = orientation.rawValue
        return orientationValToProcOrientation(val, facing)
    }
    
    public static func getOrientation() -> CameraOrientation
    {
        let facing = false
        let orient = UIDevice.current.orientation
        switch(orient)
        {
        case .portrait:
            return .DEG_0
        case .portraitUpsideDown:
            return .DEG_180
        case .landscapeRight:
            return (facing) ? .DEG_90 : .DEG_270
        case .landscapeLeft:
            return (facing) ? .DEG_90 : .DEG_270
        default:
            return .DEG_0
        }
    }
}

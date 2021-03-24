//
//  ImageUtils.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/23/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class ImageUtils
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
    
    static func findFloatMargins(size:Size, zoom:Float) -> MarginFloat
    {
        let margins = findMargins(size: size, zoom: zoom)
        var ret = MarginFloat(left: 0, top: 0, right: 0, bottom: 0, widthPitch: 0, heightPitch: 0)
        
        ret.left = Float(margins.left) / Float(size.width)
        ret.top = Float(margins.top) / Float(size.height)
        ret.right = (Float(size.width) - Float(margins.right)) / Float(size.width)
        ret.bottom = (Float(size.height) - Float(margins.bottom)) / Float(size.height)
        ret.widthPitch = (ret.right - ret.left) / Float(margins.width)
        ret.heightPitch = (ret.bottom - ret.top) / Float(margins.height)

        return ret
    }
    
    static func findMargins(size:Size, zoom:Float) -> Margin
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
            ret.width = size.width - 2 * hPaddingPx
            ret.height = size.height - 2 * wPaddingPx
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
        let status = CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32BGRA, nil, &ret)
        guard ret != nil else { return nil }
        CVPixelBufferLockBaseAddress(ret!, CVPixelBufferLockFlags())
        var retPtr = CVPixelBufferGetBaseAddress(ret!)
        
        memcpy(retPtr, ptr, height * widthSz)
        CVPixelBufferUnlockBaseAddress(base, CVPixelBufferLockFlags())
        CVPixelBufferUnlockBaseAddress(ret!, CVPixelBufferLockFlags())
        
        return ret
    }
}

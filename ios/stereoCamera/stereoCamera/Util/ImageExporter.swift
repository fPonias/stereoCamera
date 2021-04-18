//
//  ImageExporter.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/17/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage
import UIKit

class ImageExporter {
    let leftData:ImageEditorData
    let rightData:ImageEditorData
    var debugView:UIImageView?
    
    init(leftData:ImageEditorData, rightData:ImageEditorData){
        self.leftData = leftData
        self.rightData = rightData
    }
    
    private func processSide(data:ImageEditorData, size:CGSize) -> CVPixelBuffer? {
        guard let imgBig = data.process(type: .EXPORT) else { return nil }
        
        let scaler = ScaleFilter(value: Float(size.width))
        guard let img = scaler.update(imgBig) else { return nil }
        
        var ret:CVPixelBuffer?
        let _ = CVPixelBufferCreate(kCFAllocatorDefault, Int(size.width), Int(size.height), kCVPixelFormatType_32BGRA, [kCVPixelBufferMetalCompatibilityKey: true] as CFDictionary, &ret)
        guard ret != nil else { return nil }
        
        let ctx = CIContext()
        CVPixelBufferLockBaseAddress(ret!, CVPixelBufferLockFlags())
            ctx.render(img, to: ret!)
        CVPixelBufferUnlockBaseAddress(ret!, CVPixelBufferLockFlags())
        
        return ret!
    }
    
    public func export() {
        let context = CIContext()
        let leftMargin = leftData.margins
        let rightMargin = rightData.margins
        
        let dim = (leftMargin.width < rightMargin.width) ? leftMargin.width : rightMargin.width
        
        let sz = CGSize(width: CGFloat(dim), height: CGFloat(dim))
        guard let leftImg = processSide(data: leftData, size:sz) else { return }
        guard let rightImg = processSide(data: rightData, size:sz) else { return }
        let outSz = ImageUtils.Size(width: dim * 2, height: dim)
        
        DispatchQueue.main.async {
            let cimg = CIImage(cvImageBuffer: rightImg)
            self.debugView?.image = UIImage(ciImage: cimg)
        }
        
        let proc = ImageProcessorSplit(size: outSz)
        proc.setPixels(pixels: leftImg)
        proc.processCurrentInTexture(.LEFT)
        proc.setPixels(pixels: rightImg)
        proc.processCurrentInTexture(.RIGHT)
        
        guard let img = proc.getOutput(),
              let cs = CGColorSpace(name: CGColorSpace.sRGB)
        else { return }
        
        let jpegData = context.jpegRepresentation(of: img, colorSpace: cs, options: [:])
        guard let data = jpegData else { return }
        
        Files.instance.saveToPhotos(data: data, onSaved: { savedImg in
            print ("saved successfully")
        })
    }
}

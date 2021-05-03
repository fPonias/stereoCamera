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
        
        guard let buffer = data.origData else { return nil }
        
        var img:CIImage? = CIImage(cvPixelBuffer: buffer)
        let squareFilter = SquareFilter(orientation: .DEG_0, zoom: 1.0)
        img = squareFilter.update(img!)
        
        let rotateFIlter = RotateFilter(rotation: data.rotation, dimension: Int(size.width))
        img = rotateFIlter.update(img!)
        
        let gammaFilter = GammaFilter(value: 2.2)
        img = gammaFilter.update(img!)
        
        guard let imgout = img else { return nil }
        
        let ctx = CIContext()
        let sz = imgout.extent
        var outBuf:CVPixelBuffer?
        CVPixelBufferCreate(kCFAllocatorDefault, Int(size.width), Int(size.height), kCVPixelFormatType_32BGRA, [kCVPixelBufferMetalCompatibilityKey: true] as CFDictionary, &outBuf)
        
        guard let outBufConst = outBuf else { return nil }
        
        ctx.render(imgout, to: outBufConst)
        
        return outBufConst
    }
    
    public func export() {
        let context = CIContext()
        
        let leftMargin = leftData.margins
        let rightMargin = rightData.margins
        
        let dim = (leftMargin.width < rightMargin.width) ? leftMargin.width : rightMargin.width
        
        let sz = CGSize(width: CGFloat(dim), height: CGFloat(dim))
        let outSz = ImageUtils.Size(width: dim * 2, height: dim)
        
        guard let leftImg = leftData.origData,
              let rightImg = rightData.origData
        else { return }
        
        let proc = ImageProcessorSplit(outSize: outSz)
        proc.setPixels(pixels: leftImg, rotation: leftData.rotation)
        proc.processCurrentInTexture(.LEFT)
        proc.setPixels(pixels: rightImg, rotation: rightData.rotation)
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

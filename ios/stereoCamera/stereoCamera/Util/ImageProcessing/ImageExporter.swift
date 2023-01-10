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
    
    let processType:ImageFormat
    let outputQuality:ImageQuality
    
    init(leftData:ImageEditorData, rightData:ImageEditorData, processType:ImageFormat = .SPLIT, outputQuality:ImageQuality = .ULTRA_HI_DEF){
        self.leftData = leftData
        self.rightData = rightData
        self.processType = processType
        self.outputQuality = outputQuality
    }
    
    private var context = CIContext()
    private var outSz:ImageUtils.Size?
    private var proc:ImageProcessor?
    
    private func setupProcessor() {
        let leftMargin = leftData.margins
        let rightMargin = rightData.margins
        
        let maxDim = (leftMargin.width < rightMargin.width) ? leftMargin.width : rightMargin.width
        let prefDim = outputQuality.toInt()
        let dim = min(maxDim, prefDim)
        
        if (processType == .SPLIT) {
            outSz = ImageUtils.Size(width: dim * 2, height: dim)
            proc = ImageProcessorSplit(outSize: outSz!)
        } else {
            outSz = ImageUtils.Size(width: dim, height: dim)
            
            if (processType == .GREEN_MAGENTA) {
                proc = ImageProcessorGreenMagenta(outSize: outSz!)
            } else if (processType == .RED_BLUE) {
                proc = ImageProcessorRedCyan(outSize: outSz!)
            } else if (processType == .ANIMATED) {
                proc = ImageProcessorAnimatedGif(outSize: outSz!, frameDelay: 0.1)
            } else if (processType == .SINGLE) {
                proc = ImageProcessorSingle(outSize: outSz!)
            } else {
                return
            }
        }
    }
    
    public func export() {
        guard let leftImg = leftData.origData,
              let rightImg = rightData.origData
        else { return }
        
        setupProcessor()
        
        guard let proc = proc else { return }
        
        proc.setPixels(pixels: leftImg, margins: leftData.margins, rotation: leftData.rotation, offset: CGPoint(x: 0, y: 0))
        proc.processCurrentInTexture(.LEFT)
        proc.setPixels(pixels: rightImg, margins: rightData.margins, rotation: rightData.rotation, offset: CGPoint(x: 0, y: 0))
        proc.processCurrentInTexture(.RIGHT)
        
        guard let data = proc.getFinalImageData(),
              let data2 = proc.addMetaData(data)
        else { return }
        let ext = proc.getFileExtension()
        
        Files.instance.saveImageToPhotos(data: data2, extension: ext, onSaved: { savedImg in
            print ("saved successfully")
        })
    }
}

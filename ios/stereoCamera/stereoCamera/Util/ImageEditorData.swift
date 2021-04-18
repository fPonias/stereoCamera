//
//  ImageEditorData.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/17/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class ImageEditorData //mutable struct
{
    init(origData:CVPixelBuffer, zoom: Float, orientation: ImageUtils.CameraOrientation) {
        self.origData = ImageUtils.copyBuffer(base: origData)
        self.zoom = zoom
        self.orientation = orientation
        
        let cropWidget = SquareFilter(orientation: orientation, zoom: zoom)
        automaticFilters.append(cropWidget)
        
        let gammaWidget = GammaFilter(value: 2.2)
        processFilters.append(gammaWidget)
    }
    
    var origData: CVPixelBuffer?
    var zoom: Float
    var orientation: ImageUtils.CameraOrientation
    
    var automaticFilters:[ImageEditorFilter] = Array()
    var manualFilters:[ImageEditorWidget] = Array()
    var previewFilters:[ImageEditorFilter] = Array()
    var processFilters:[ImageEditorFilter] = Array()
    
    var margins:ImageUtils.Margin {
        get {
            let w:Int
            let h:Int
            if (origData != nil) {
                w = CVPixelBufferGetWidth(origData!)
                h = CVPixelBufferGetHeight(origData!)
            } else {
                w = 0
                h = 0
            }
            
            let sz:ImageUtils.Size
            if orientation == ImageUtils.CameraOrientation.DEG_0 || orientation == .DEG_180 {
                sz = ImageUtils.Size(width: w, height: h)
            } else {
                sz = ImageUtils.Size(width: h, height: w)
            }
            
            return ImageUtils.findMargins(size: sz, zoom: zoom)
        }
    }
    
    enum ProcessType {
        case PREVIEW
        case EXPORT
    }
    
    func process(type: ProcessType) -> CIImage? {
        guard let origData = origData else { return nil }
        var ret:CIImage? = CIImage(cvPixelBuffer: origData)
        
        if (type == .EXPORT) {
            ret = applyFilters(img: ret, arr: processFilters)
        } else {
            ret = applyFilters(img: ret, arr: previewFilters)
        }
        
        ret = applyFilters(img: ret, arr: automaticFilters)
        ret = applyFilters(img: ret, arr: manualFilters)
        
        return ret
    }
    
    private func applyFilters(img: CIImage?, arr:[ImageEditorFilter]) -> CIImage? {
        var ret:CIImage? = img
        
        for filter in arr {
            guard (ret != nil) else { return nil }
            ret = filter.update(ret!)
        }
        
        return ret
    }
}

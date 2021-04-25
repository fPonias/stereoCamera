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
    init(origData:CVPixelBuffer, zoom: Float, rotation: Float) {
        self.origData = ImageUtils.copyBuffer(base: origData)
        self.zoom = zoom
        self.margins = ImageEditorData.getMargin(origData, zoom: zoom)
        self.rotation = rotation
        
        let cropWidget = SquareFilter(orientation: .DEG_0, zoom: zoom)
        automaticFilters.append(cropWidget)
        let rotateWidget = RotateFilter(rotation: rotation, dimension: margins.width)
        automaticFilters.append(rotateWidget)
        
        let gammaWidget = GammaFilter(value: 2.2)
        processFilters.append(gammaWidget)
    }
    
    var origData: CVPixelBuffer?
    var zoom: Float
    var rotation: Float
    
    var automaticFilters:[ImageEditorFilter] = Array()
    var manualFilters:[ImageEditorWidget] = Array()
    var previewFilters:[ImageEditorFilter] = Array()
    var processFilters:[ImageEditorFilter] = Array()
    
    var margins:ImageUtils.Margin
    
    private static func getMargin(_ origData:CVPixelBuffer, zoom:Float) -> ImageUtils.Margin {

        let w = CVPixelBufferGetWidth(origData)
        let h = CVPixelBufferGetHeight(origData)
        
        let sz:ImageUtils.Size = ImageUtils.Size(width: h, height: w)
        return ImageUtils.findMargins(size: sz, zoom: zoom)
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

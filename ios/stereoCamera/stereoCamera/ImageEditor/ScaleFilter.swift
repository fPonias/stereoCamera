//
//  CropWidget.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/17/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class ScaleFilter : ImageEditorFilter
{
    override init(value: Float) {
        super.init(value: value)
        
        filter = CIFilter.init(name: "CIBicubicScaleTransform")
    }
    
    override func update(_ img:CIImage) -> CIImage? {
        filter.setValue(img, forKey: "inputImage")
        filter.setValue(1.0, forKey: "inputAspectRatio")
        filter.setValue(0.0, forKey: "inputB")
        filter.setValue(0.75, forKey: "inputC")
        filter.setValue(value, forKey: "inputScale")
        
        return filter.outputImage
    }
    
    override func toString() -> String {
        return "preview scale"
    }
}

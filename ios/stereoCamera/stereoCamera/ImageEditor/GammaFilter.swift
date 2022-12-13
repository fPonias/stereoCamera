//
//  GammaWidget.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/17/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class GammaFilter : ImageEditorFilter
{
    override init(value:Float) {
        super.init(value: value)
        filter = CIFilter.init(name: "CIGammaAdjust")
    }
    
    override func update(_ img:CIImage) -> CIImage? {
        filter.setValue(img, forKey: "inputImage")
        filter.setValue(_value, forKey: "inputPower")
        return filter.outputImage
    }
    
    override func toString() -> String {
        return "gamma filter value " + String(_value)
    }
}

//
//  ImagePostProcessor.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/13/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class ImagePostProcessor {
    let image:CIImage
    
    let gammaFilter:CIFilter!
    
    init(image:CIImage) {
        self.image = image
        output = image
        
        gammaFilter = CIFilter.init(name: "CIGammaAdjust")
    }
    
    private (set) var output:CIImage?
    
    func gamma() {
        gammaFilter.setValue(output, forKey: kCIInputImageKey)
        gammaFilter.setValue(1.25, forKey: "inputPower")
        
        output = gammaFilter.outputImage
    }
}

//
//  OffsetFilter.swift
//  stereoCamera
//
//  Created by Cody Munger on 6/18/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
//
//  GammaWidget.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/17/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class OffsetFilter : ImageEditorFilter
{
    var transform:CGAffineTransform
    var offset:CGPoint
    var filter2:CIFilter!
    
    init(offset:Int) {
        transform = CGAffineTransform.identity
        self.offset = CGPoint(x: 0, y: offset)
        
        super.init(value: 0)
        
        filter = CIFilter.init(name: "CIAffineTransform")
        filter2 = CIFilter.init(name: "CICrop")
    }
    
    init(offset:CGPoint) {
        transform = CGAffineTransform.identity
        self.offset = offset
        
        super.init(value: 0)
        
        filter = CIFilter.init(name: "CIAffineTransform")
        filter2 = CIFilter.init(name: "CICrop")
    }
    
    override func update(_ img:CIImage) -> CIImage? {
        filter.setValue(img, forKey: "inputImage")
        
        transform = CGAffineTransform.identity
        transform = transform.translatedBy(x: offset.x, y: offset.y)
        
        filter.setValue(transform, forKey: "inputTransform")
        let ret = filter.outputImage
        
        var rect = img.extent
        rect.size.height -= CGFloat(offset.y)
        rect.size.width -= CGFloat(offset.x)
        
        let arg = CIVector(cgRect: rect)
        filter2.setValue(ret, forKey: "inputImage")
        filter2.setValue(arg, forKey: "inputRectangle")
        
        return filter2.outputImage
    }
    
    override func toString() -> String {
        return "offset"
    }
}

//
//  RotationWidget.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/17/21.
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

class SquareFilter : ImageEditorFilter
{
    var transform:CGAffineTransform
    var orientation:ImageUtils.CameraOrientation
    var rotate:CGFloat
    var zoom:Float
    
    init(orientation: ImageUtils.CameraOrientation, zoom:Float) {
        transform = CGAffineTransform.identity
        self.orientation = orientation
        rotate = ImageUtils.orientationToRadians(orientation)
        self.zoom = zoom
        
        super.init(value: 0)
        
        filter = CIFilter.init(name: "CIAffineTransform")
    }
    
    override func update(_ img:CIImage) -> CIImage? {
        filter.setValue(img, forKey: "inputImage")
        
        transform = CGAffineTransform.identity
        
        let rect = img.extent
        let halfw = rect.width / 2.0
        let halfh = rect.height / 2.0
        transform = transform.translatedBy(x: halfw, y: halfh).rotated(by: rotate).translatedBy(x: -halfw, y: -halfh)
        
        let sz:ImageUtils.Size
        if orientation == ImageUtils.CameraOrientation.DEG_0 || orientation == .DEG_180 {
            sz = ImageUtils.Size(width: Int(rect.width), height: Int(rect.height))
        } else {
            sz = ImageUtils.Size(width: Int(rect.height), height: Int(rect.width))
        }
        
        let margins = ImageUtils.findMargins(size: sz, zoom: zoom)
        transform = transform.translatedBy(x: CGFloat(-margins.left), y: CGFloat(-margins.top))
        
        filter.setValue(transform, forKey: "inputTransform")
        return filter.outputImage
    }
    
    override func toString() -> String {
        return "rotation"
    }
}

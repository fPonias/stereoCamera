//
//  RotateFilter.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/25/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

class RotateFilter : ImageEditorFilter
{
    var transform:CGAffineTransform
    var rotate:Float
    var zoom:Float
    var dimension:Int
    
    init(rotation:Float, dimension:Int) {
        transform = CGAffineTransform.identity
        rotate = rotation
        self.dimension = dimension
        zoom = 1.0
        
        super.init(value: 0)
        
        filter = CIFilter.init(name: "CIAffineTransform")
    }
    
    override func update(_ img:CIImage) -> CIImage? {
        filter.setValue(img, forKey: "inputImage")
        
        //plots the arc of a circle traced by a square of width 2 centered on 0,0
        let rad:Float = sqrt(2.0 * 1.0) //the circle's radius is sqrt(2 * w ^ 2) - from c^2 = a^2 + b^2
        let fortyFive = Float.pi / 4.0
        let ninety = Float.pi / 2.0
        let rot45 = (rotate - fortyFive).remainder(dividingBy: ninety) //constrain the input rotation to -45 to 45 degrees
        let x = sin(rot45) * rad // find the x coordinate from the input rotation
        let ysq = rad * rad - x * x //find the y coordinate of the arc equation sqrt(radius^2 - x^2)
        let zoom = sqrt(ysq) //zoom should be a number between 1.0 and the radius 1.4
        //print ("rot \(x) zoom \(zoom)")
        
        var rot:Float = -Float.pi / 2.0
        rot += rotate
        //print ("debug rot \(rot)")
        
        transform = CGAffineTransform.identity
        
        let halfw = CGFloat(dimension) / 2.0
        let halfh = CGFloat(dimension) / 2.0
        transform = transform
            .translatedBy(x: halfw, y: halfh)
            .rotated(by: CGFloat(rot))
            .scaledBy(x: CGFloat(zoom), y: CGFloat(zoom))
            .translatedBy(x: -halfw, y: -halfh)
        
        filter.setValue(transform, forKey: "inputTransform")
        let ret = filter.outputImage
        
        
        
        return ret
    }
    
    override func toString() -> String {
        return "free rotation"
    }
}

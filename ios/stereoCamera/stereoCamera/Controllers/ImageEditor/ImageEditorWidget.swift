//
//  ImageEditorWidget.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/13/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage

open class ImageEditorFilter
{
    init(value:Float) {
        _value = value
    }
    
    var filter:CIFilter!
    var _value:Float = 1.0
    public var value:Float {
        get { return _value }
        set(val) { _value = val }
    }
    
    func update(_ img:CIImage) -> CIImage? { return nil }
    func toString() -> String { return "" }
}

open class ImageEditorWidget : ImageEditorFilter
{
    public var min:Float = 0.0
    public var max:Float = 2.0
    
    init(min:Float, max:Float) {
        super.init(value: min)
        self.min = min
        self.max = max
    }
}

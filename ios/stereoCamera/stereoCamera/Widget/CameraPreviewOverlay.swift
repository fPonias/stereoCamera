//
//  CameraPreviewOverlay.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/27/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class CameraPreviewOverlay : UIView
{
    private var _overlay:Overlay = Overlay.NONE
    var overlay:Overlay
    {
        get { return _overlay}
        set { _overlay = newValue }
    }

    override func draw(_ rect: CGRect)
    {
        switch(_overlay)
        {
            case .NONE:
                return
            case .HALF:
                drawGrid(rect: rect, divs: 2)
            case .THIRDS:
                drawGrid(rect: rect, divs: 3)
            case .FOURTHS:
                drawGrid(rect: rect, divs: 4)
        }
    }
    
    func drawGrid(rect: CGRect, divs: Int)
    {
        UIColor.black.setFill()
        let delta = Int(rect.width) / divs
        
        for i in 0 ... divs - 1
        {
            let pathy = UIBezierPath(rect: CGRect(x: delta * i, y: 0, width: 1, height: Int(rect.height)))
            pathy.fill()
            let pathx = UIBezierPath(rect: CGRect(x: 0, y: delta * i, width: Int(rect.width), height: 1))
            pathx.fill()
        }
    }
}

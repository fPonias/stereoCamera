//
//  GraphPlot.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/19/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class PlotCell : UICollectionViewCell {
    
    @IBOutlet weak var plot: HistogramPlot!
}

class HistogramPlot : UIView {
    
    private var _data:[[Float]] = Array()
    
    private var _maxX:Int = 0
    private var _maxY:Float = 0
    private var _minY:Float = 0
    
    public func appendData(_ data:[Float]) {
        _data.append(data)
        _maxX = max(_maxX, data.count)
        _maxY = max(_maxY, data.max() ?? 0)
        _minY = min(_minY, data.min() ?? 0)
        
        setNeedsDisplay()
    }
    
    override func draw(_ rect: CGRect)
    {
        super.draw(rect)
        
        guard let ctx = UIGraphicsGetCurrentContext() else { return }
        
        guard _maxX > 0 else { return }
        let xWidth = rect.width
        let xDelta = CGFloat(xWidth) / CGFloat(_maxX)
        
        let yHeight = rect.height
        let yDelta = CGFloat(yHeight) / CGFloat(_maxY - _minY)
        
        for i in 0 ..< _data.count {
            if i == 0 {
                ctx.setFillColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1.0 / CGFloat(_data.count))
            } else {
                ctx.setFillColor(red: 1.0, green: 0.1, blue: 0.1, alpha: CGFloat(_data.count))
            }
            
            for x in 0 ..< _maxX {
                let sz = CGSize(width: xDelta, height: yDelta * (CGFloat(_data[i][x]) - CGFloat(_minY)))
                let origin = CGPoint(x: xDelta * CGFloat(x), y: rect.height - sz.height)
                ctx.fill(CGRect(origin: origin, size: sz))
            }
        }
    }
}

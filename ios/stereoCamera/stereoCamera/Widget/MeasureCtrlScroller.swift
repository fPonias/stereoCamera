//
//  MeasureCtrlScroller.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation

class MeasureCtrlScroller : UIScrollView
{
    func reset() {
        for v in subviews {
            v.removeFromSuperview()
        }
        
        datum.removeAll()
    }
    
    private var datum:[[[Float]]] = Array()
    private var totalWidth:CGFloat = 0.0
    
    func append(data:[[Float]], size:CGSize) {
        let x = totalWidth + 10.0
        let y = CGFloat(0)
        let w = size.width
        let h = size.height
        let histView = HistogramPlot(frame: CGRect(x: x, y: y, width: w, height: h))
        histView.backgroundColor = UIColor.white
        
        for d in data {
            histView.appendData(d)
        }
        
        totalWidth += size.width + 10
        datum.append(data)
        
        contentSize = CGSize(width: totalWidth, height: max(contentSize.height, size.height + 10))
        
        addSubview(histView)
    }
}

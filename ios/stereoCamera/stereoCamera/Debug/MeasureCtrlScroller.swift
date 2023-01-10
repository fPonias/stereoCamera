//
//  MeasureCtrlScroller.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

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
    
    func append(leftPixels: UIImage, rightPixels: UIImage, data:[[Float]], size:CGSize) {
        let x = totalWidth + 10.0
        var y = CGFloat(0)
        let w = size.width
        let h = size.height / 3.0
        
        var frame = CGRect(x: x, y: y, width: w, height: h)
        appendHistogram(data: data, frame: frame)
        
        y = CGFloat(h)
        frame = CGRect(x: x, y: y, width: w, height: h)
        var view = UIImageView(frame: frame)
        view.image = leftPixels
        addSubview(view)
        
        y = CGFloat(h * 2)
        frame = CGRect(x: x, y: y, width: w, height: h)
        view = UIImageView(frame: frame)
        view.image = rightPixels
        addSubview(view)
        
        totalWidth += size.width + 10
        contentSize = CGSize(width: totalWidth, height: max(contentSize.height, size.height + 10))
    }
    
    private func appendHistogram(data:[[Float]], frame:CGRect) {
        let histView = HistogramPlot(frame: frame)
        histView.backgroundColor = UIColor.white
        
        for d in data {
            histView.appendData(d)
        }
        
        datum.append(data)
        
        addSubview(histView)
    }
    
    func appendImage(pixels: CVPixelBuffer, frame: CGRect) {
        let ciImage = CIImage(cvPixelBuffer: pixels)
        let uiImage = UIImage(ciImage: ciImage)
        let view = UIImageView(frame: frame)
        view.image = uiImage
        addSubview(view)
    }
}

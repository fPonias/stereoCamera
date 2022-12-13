//
//  MeaureCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/18/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class MeasureCtrl : UIViewController
{
    var leftPixels:CVPixelBuffer?
    var rightPixels:CVPixelBuffer?
    var zoom:Float = 1.0
    
    @IBOutlet weak var histogramView: HistogramPlot!
    
    @IBOutlet weak var summaryView: MeasureCtrlScroller!
    
    private var leftData:[Float]?
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    @IBAction func histogramBtn(_ sender: Any)
    {
        guard let leftPixels = leftPixels,
              let rightPixels = rightPixels
        else { return }
        
        let histogram = Histogram()
        histogram.setPixels(pixels: leftPixels)
        histogram.setZoom(1.0)
        guard let dataRough = histogram.calculate() else { return }
        let data = histogram.smooth(data: dataRough)
        
        leftData = Array(repeating: Float(0), count: data.count)
        for i in 0 ..< data.count {
            leftData![i] = Float(data[i])
        }
        
        histogramView.appendData(leftData!)
                
        let histogram2 = Histogram()
        histogram2.setPixels(pixels: rightPixels)
        histogram2.setZoom(zoom)
        
        
        var margin1 = histogram.getMargins()
        let height1 = CVPixelBufferGetHeight(leftPixels) - margin1.top - margin1.bottom
        var margin2 = histogram2.getMargins()
        let height2 = CVPixelBufferGetHeight(rightPixels) - margin2.top - margin2.bottom
        
        var arr1:[[Float]] = Array()
        var arr2:[[Float]] = Array()
        let offset1:Int = 10
        let offset2:Float = 10.0 * Float(height2) / Float(height1)
        
        for offset in stride(from: -50, to: 50, by: 5) {
            var margin2 = histogram2.getMargins()
            margin2.bottom += Int(offset)
            margin2.top -= Int(offset)
            histogram2.setMargins(margin2)
            
            var margin1 = histogram.getMargins()
            histogram.setMargins(margin1)
            
            guard let data2 = histogram2.calculate(),
                  let data1 = histogram.calculate()
            else { break }
            
            var data1Count:Int32 = 0
            var data2Count:Int32 = 0
            for i in 0 ..< data1.count {
                data1Count += data1[i]
                data2Count += data2[i]
            }
            
            let ratio = Float(data1Count) / Float(data2Count)
            
            var data1f:[Float] = Array()
            for data1Item in data1 {
                data1f.append(Float(data1Item))
            }
            arr1.append(data1f)
            
            var data2f:[Float] = Array()
            for data2Item in data2 {
                let data2Val = Float(data2Item) * ratio
                data2f.append(data2Val)
            }
            arr2.append(data2f)
        }
        
        summaryView.reset()
        
        for i in 0 ..< arr1.count {
            summaryView.append(data:[arr1[i],arr2[i]], size: histogramView.frame.size)
        }
    }
    
    func diff(orig:[Float], zoomed:[Float], ratio:Float) -> Float {
        var ret = Float(0.0)
        
        for i in 0 ..< orig.count {
            let d = Float(orig[i]) - ratio * Float(zoomed[i])
            ret += d * d
        }
        
        return ret
    }
}

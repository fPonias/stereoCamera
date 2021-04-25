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
        
        
        var leftCount = Float(0)
        for i in 0 ..< leftData!.count {
            leftCount += Float(leftData![i])
        }
        
        let histogram2 = Histogram()
        histogram2.setPixels(pixels: rightPixels)
        var arr:[[Float]] = Array()
        var diffs:[Float] = Array()
        var lows:[Float] = Array()
        var zoom:Float = 1.0
        let step:Float = 0.025
        var decending = true
        while zoom <= 2.25 {
            histogram2.setZoom(zoom)
            guard let data = histogram2.calculate() else { return }
            //let data = histogram2.smooth(data: dataRough)
            
            var rightCount = Float(0)
            let sz = data.count
            var dataCopy = Array(repeating: Float(0), count: sz)
            for i in 0 ..< sz {
                dataCopy[i] = Float(data[i])
                rightCount += Float(data[i])
            }
            
            let ratio = leftCount / rightCount
            
            for i in 0 ..< sz {
                dataCopy[i] = ratio * dataCopy[i]
            }
            
            arr.append(dataCopy)
            diffs.append(diff(orig: leftData!, zoomed: dataCopy, ratio: 1.0))
            let sz2 = diffs.count
            if (sz2 > 1) {
                if (decending && diffs[sz2 - 1] > diffs[sz2 - 2]) {
                    lows.append(zoom)
                    decending = false
                    print ("low found at \(zoom)")
                } else if (!decending && diffs[sz2 - 1] < diffs[sz2 - 2]) {
                    decending = true
                }
            }
            zoom += step
        }
        
        summaryView.reset()
        let arg1:[[Float]] = [diffs]
        //summaryView.append(data:arg1, size: histogramView.frame.size)
        
        for i in 0 ..< arr.count {
            summaryView.append(data:[leftData!,arr[i]] as [[Float]], size: histogramView.frame.size)
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

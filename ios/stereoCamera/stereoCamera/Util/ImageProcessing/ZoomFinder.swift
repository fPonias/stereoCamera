//
//  ZoomFinder.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class ZoomFinder {
    let baseHist = ReducedHistogram()
    let adjHist = Histogram()
    private let queue = DispatchQueue(label: "Zoom finder thread")
    private var isRunning = false
    
    init()
    {
    }
    
    func canFindZoom() -> Bool
    {
        return baseHist.hasTexture() && adjHist.hasTexture()
    }
    
    private var max:Float = 1.0
    
    func findZoom(max:Float, callback: @escaping (Float) -> Void)
    {
        self.max = max
        queue.sync {
            if (isRunning) {
                return
            }
            
            isRunning = true
            
            queue.async {
                self.findZoomPriv(callback)
            }
        }
    }
    
    private let searchDivs = 4
    
    private func findZoomPriv(_ callback: (Float)->Void) {
        baseHist.setZoom(1.0, offset: CGPoint())
        
        let ret = findZoomLin(left: 1.0, right: max, step: 0.1)
        if (ret.peak == 0 || ret.valley == Float.infinity) {
            callback(1.0)
        } else {
            let right = ret.peak + (ret.valley - ret.peak) * 2
            let ret2 = findZoomRec(left: ret.peak, right: right)
            callback(ret2)
        }
        
        isRunning = false
    }
    
    private func findZoomRec(left: Float, right: Float) -> Float
    {
        if (right - left < 0.001) {
            return left + (right - left) / 2.0
        }
        
        let step = (right - left) / 5.0
        var min = Int.max
        var ret = left
        
        for i in 0 ..< 5 {
            let idx = Float(i) * step + left
            let val = getDiff(zoom: idx, bucketSize: 1)
            if val < min {
                min = val
                ret = idx
            } else {
                break
            }
        }
        
        if ret == left {
            return ret
        }

        return findZoomRec(left: ret - step, right: ret + step)
    }
    
    struct zoomLimits {
        var peak: Float
        var valley: Float
    }
    
    private let bucketSizes:[Int] = [1, 2, 4, 8, 16, 32, 64, 128, 256, 512]
    
    private func findZoomLin(left: Float, right: Float, step: Float) -> zoomLimits {
        var ret = zoomLimits(peak: 0, valley: Float.infinity)
        var peak = 0
        var valley = Int.max
        var findingPeak = true
        var findingValley = false
        var arr = Array<Int>()
        var str = ""
        
        var i = left
        while i < right {
            let diff = getDiff(zoom: i, bucketSize: 2)
            
            arr.append(diff)
            
            if findingPeak {
                if diff > peak {
                    peak = diff
                    ret.peak = i
                } else {
                    findingPeak = false
                    findingValley = true
                }
            } else if findingValley {
                if (diff < valley) {
                    valley = diff
                    ret.valley = i
                } else {
                    findingValley = false
                    //return ret
                }
            }
            
            str += "(" + String(i) + ", " + String(diff) + ") "
            
            i += step
        }
        
        print(str)
        return ret
    }
    
    private func getDiff(zoom:Float, bucketSize:Int) -> Int {
        baseHist.setZoom(1.0, offset: CGPoint())
        guard let baseVal = baseHist.calculate() else { return Int.max }
        adjHist.setZoom(zoom)
        guard let adjVal = adjHist.calculate() else { return Int.max }
        
        var diff:Float = 0.0
        var aCount:Float = 0.0
        var bCount:Float = 0.0
        
        for i in 0 ..< adjVal.count {
            let baseItem = baseVal[i]
            bCount += Float(baseItem)
            let adjItem = adjVal[i]
            aCount += Float(adjItem)
        }
        
        let ratio = bCount / aCount
        aCount = 0
        bCount = 0
        
        for i in 0 ..< adjVal.count {
            let baseItem = baseVal[i]
            let adjItem = adjVal[i]
            
            aCount += Float(adjItem) * ratio
            bCount += Float(baseItem)
        }
        
        let sz = adjVal.count
        var baseItem:Float = 0.0
        var adjItem:Float = 0.0
        for i in 0 ..< sz {
            if i % bucketSize == 0 {
                let diffFl = adjItem * ratio - baseItem
                diff += abs(diffFl)
                
                baseItem = 0.0
                adjItem = 0.0
            }
            
            baseItem += Float(baseVal[i])
            adjItem += Float(adjVal[i])
        }
        
        if (baseItem > 0 || adjItem > 0) {
            let diffFl = adjItem * ratio - baseItem
            diff += abs(diffFl)
        }
        
        return Int(diff)
    }
}

//
//  ZoomFinder.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation

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
        baseHist.setZoom(1.0)
        
        let ret = findZoomLin(left: 1.0, right: max, step: (max - 1.0) / 10.0)
        //let ret2 = findZoomRec(left: 1.0, right: max)
        callback(ret)
        
        isRunning = false
    }
    
    private func findZoomLin(left: Float, right: Float, step: Float) -> Float {
        var ret = left
        var minVal = Float.infinity
        var arr = Array<Float>()
        var str = ""
        
        var i = left
        while i < right {
            let diff = getDiff(zoom: i)
            arr.append(diff)
            
            if diff < minVal {
                minVal = diff
                ret = i
            }
            
            str += "(" + String(i) + ", " + String(diff) + ") "
            
            i += step
        }
        
        print(str)
        return ret
    }
    
    private func getDiff(zoom:Float) -> Float {
        baseHist.setZoom(zoom)
        guard let baseVal = baseHist.calculate() else { return Float.infinity }
        adjHist.setZoom(zoom)
        guard let adjVal = adjHist.calculate() else { return Float.infinity }
        
        var diff = 0
        var aCount = 0
        var bCount = 0
        
        for i in 0 ..< adjVal.count {
            let baseItem = baseVal[i]
            bCount += Int(baseItem)
            let adjItem = adjVal[i]
            aCount += Int(adjItem)
            let diffItem = Int(baseItem) - Int(Float(adjItem))
            diff += abs(diffItem)
        }
        
        let ratio = Float(diff) / Float(bCount)
        return ratio
    }
}

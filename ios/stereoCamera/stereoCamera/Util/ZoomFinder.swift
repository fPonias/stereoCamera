//
//  ZoomFinder.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation

class ZoomFinder {
    let baseHist = Histogram()
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
    
    func findZoom(callback: @escaping (Float) -> Void)
    {
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
    
    private var baseVal: [Int32]?
    private let searchDivs = 4
    
    private func findZoomPriv(_ callback: (Float)->Void) {
        baseHist.setZoom(1.0)

        guard let baseVal = baseHist.calculate() else {
            callback(1.0)
            return
        }
        
        self.baseVal = baseVal
        //let ret = findZoomLin(left: 1.0, right: 10.0, step: 0.5)
        let ret2 = findZoomRec(left: 1.0, right: 10.0)
        callback(ret2)
        
        isRunning = false
    }
    
    private func findZoomLin(left: Float, right: Float, step: Float) -> Float {
        guard let baseVal = baseVal else { return left }
        
        var ret = left
        var minVal = Int.max
        var arr = Array<Int>()
        var str = ""
        
        var i = left
        while i < right {
            let diff = getDiff(baseVal, zoom: i)
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
    
    private func findZoomRec(left: Float, right: Float) -> Float {
        guard let baseVal = baseVal else { return left }
        
        let step = (right - left) / Float(searchDivs)
        if (step < baseHist.zoomGranularity) {
            return left
        }
        
        var lastVal = 0
        var zoom = left
        
        for i in 0 ... searchDivs {
            let val = getDiff(baseVal, zoom: zoom)
            
            if (i > 0 && val > lastVal) {
                if (i == 1) {
                    return left
                } else {
                    return findZoomRec(left: zoom - (step * 2.0), right: zoom)
                }
            }
            
            lastVal = val
            zoom += step
        }
        
        return right
    }
    
    private func getDiff(_ baseVal:[Int32], zoom:Float) -> Int {
        adjHist.setZoom(zoom)
        guard let adjVal = adjHist.calculate() else { return Int.max }
        let mult = Float(baseHist.size) / Float(adjHist.size)
        var diff = 0
        
        for i in 0 ..< adjVal.count {
            let baseItem = baseVal[i]
            let adjItem = adjVal[i]
            let diffItem = Int(baseItem) - Int(Float(adjItem) * mult)
            diff += abs(diffItem)
        }
        
        return diff
    }
}

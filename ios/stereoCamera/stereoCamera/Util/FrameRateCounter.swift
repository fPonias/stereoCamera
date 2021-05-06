//
//  FrameRateCounter.swift
//  stereoCamera
//
//  Created by Cody Munger on 5/5/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation

class FrameRateCounter
{
    private var queue:[Date] = Array(repeating: Date(), count: 0)
    private var lastPrint = Date()
    
    func tick() {
        let now = Date()
        queue.append(now)
        
        while (!queue.isEmpty && now.timeIntervalSince(queue[0]) > 1) {
            queue.remove(at: 0)
        }
        
        
        if (now.timeIntervalSince(lastPrint) > 1) {
            let diff = now.timeIntervalSince(queue[0])
            let frameRate = (diff > 0) ? Double(queue.count) / Double(diff) : 0
            
            print ("frame rate: \(frameRate)")
            
            lastPrint = now
        }
    }
}

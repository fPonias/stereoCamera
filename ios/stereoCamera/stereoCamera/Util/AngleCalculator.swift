//
//  AngleCalculator.swift
//  stereoCamera
//
//  Created by Cody Munger on 5/18/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreMotion
import UIKit

class AngleCalculator
{
    private var motionArr:[Double] = Array(repeating: 0.0, count: 5)
    private let motion = CMMotionManager()
    private var forcedOrientation = UIInterfaceOrientation.portrait
    public var orientation:UIInterfaceOrientation {
        get { return forcedOrientation }
        set { forcedOrientation = newValue}
    }
    
    init() {
        motion.startDeviceMotionUpdates()
    }
    
    deinit {
        motion.stopDeviceMotionUpdates()
    }
    
    func calculate() -> Double {
        var angle:Double
        
        if motion.isDeviceMotionAvailable,
           let accel = motion.deviceMotion
        {
            //let x = accel.attitude.roll
            let y = -accel.attitude.pitch
            //let z = accel.attitude.yaw
                        
            //debug
            //let xs = NSString(format:"%.2f", x)
            //let ys = NSString(format:"%.2f", y)
            //let zs = NSString(format:"%.2f", z)
            //print ("\(xs), \(ys), \(zs)")
            
            angle = y + Double.pi / 2.0
            
        } else {
            angle = 0.0
        }
        
        
        motionArr.removeFirst()
        motionArr.append(angle)
        
        var total = 0.0
        for item in motionArr {
            total += item
        }
        
        return total / Double(motionArr.count)
    }
}

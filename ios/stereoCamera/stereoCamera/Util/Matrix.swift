//
//  Matrix.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/25/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation

struct Float3 {
    var x:Float
    var y:Float
    var z:Float
}

class Matrix {
    var m: [Float]
    
    init() {
        m = [1, 0, 0, 0,
             0, 1, 0, 0,
             0, 0, 1, 0,
             0, 0, 0, 1
        ]
    }
    
    func set(_ index:Int, _ value:Float) {
        m[index] = value
    }
    
    func translate(_ position: Float3){
        m[12] = position.x
        m[13] = position.y
        m[14] = position.z
    }
    
    func scale(_ scale: Float) {
        m[0] = scale
        m[5] = scale
        m[10] = scale
        m[15] = 1.0
    }
    
    func rotate(_ rot: Float3) {
        m[0] = cos(rot.y) * cos(rot.z)
        m[4] = cos(rot.z) * sin(rot.x) * sin(rot.y) - cos(rot.x) * sin(rot.z)
        m[8] = cos(rot.x) * cos(rot.z) * sin(rot.y) + sin(rot.x) * sin(rot.z)
        m[1] = cos(rot.y) * sin(rot.z)
        m[5] = cos(rot.x) * cos(rot.z) + sin(rot.x) * sin(rot.y) * sin(rot.z)
        m[9] = -cos(rot.z) * sin(rot.x) + cos(rot.x) * sin(rot.y) * sin(rot.z)
        m[2] = -sin(rot.y)
        m[6] = cos(rot.y) * sin(rot.x)
        m[10] = cos(rot.x) * cos(rot.y)
        m[15] = 1.0
    }
    
    func identity() {
        for r in 0 ..< 4 {
            for c in 0 ..< 4 {
                let idx = r * 4 + c
                if (r == c) {
                    m[idx] = 1
                } else {
                    m[idx] = 0
                }
            }
        }
    }
}

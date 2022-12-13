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
    var m:[Float] = [1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1 ]
    
    init() {}
    
    init(translation position: Float3){
        m[12] = position.x
        m[13] = position.y
        m[14] = position.z
    }
    
    init(scale: Float) {
        m[0] = scale
        m[5] = scale
        m[10] = scale
        m[15] = 1.0
    }
    
    init(rotation rot: Float3) {
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
    
    func multiply(_ b:Matrix) -> Matrix {
        let ret = Matrix()
        for r in 0 ..< 4 {
            for c in 0 ..< 4 {
                var val:Float = 0.0
                for i in 0 ..< 4 {
                    let aidx = r * 4 + i
                    let bidx = i * 4 + c
                    val += m[aidx] * b.m[bidx]
                }
                
                let vidx = r * 4 + c
                ret.m[vidx] = val
            }
        }
        
        return ret
    }
}

struct Float2 {
    var x:Float
    var y:Float
}

class Matrix2D {
    var m:[Float] = [1, 0, 0,
                     0, 1, 0,
                     0, 0, 1]
    
    init() {}
    
    init(translation position: Float2){
        m[2] = position.x
        m[5] = position.y
        m[8] = 1.0
    }
    
    init(scale: Float) {
        m[0] = scale
        m[4] = scale
        m[8] = 1.0
    }
    
    init(rotation rot: Float) {
        m[0] = cos(rot)
        m[1] = sin(rot)
        m[3] = -sin(rot)
        m[4] = cos(rot)
        m[8] = 1.0
    }
    
    func multiply(_ b:Matrix2D) -> Matrix2D {
        let ret = Matrix2D()
        for r in 0 ..< 3 {
            for c in 0 ..< 3 {
                var val:Float = 0.0
                for i in 0 ..< 3 {
                    let aidx = r * 3 + i
                    let bidx = i * 3 + c
                    val += m[aidx] * b.m[bidx]
                }
                
                let vidx = r * 3 + c
                ret.m[vidx] = val
            }
        }
        
        return ret
    }
}

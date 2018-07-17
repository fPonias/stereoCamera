//
//  Angles.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/16/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class Angles
{
    static func verticalOrientation(gravity g:Gravity) -> Float
    {
        return yOrient(x:g.x, y:g.y, z:g.z) - Float.pi
    }

    static func horizontalOrientation(orientation:UIDeviceOrientation, gravity g:Gravity) -> Float
    {
        return -xOrient(x:g.x, y:g.y, z:g.z);
    }

    static func zOrient(x:Float, y:Float, z:Float) -> Float
    {
        let zval = x * x + y * y;
        let azrad = atan2(sqrt(zval), z);
        //let az = azrad * 180.0 / Float.pi;

        return azrad;
    }

    static func yOrient(x:Float, y:Float, z:Float) -> Float
    {
        let yval = x * x + z * z;
        let ayrad = atan2(sqrt(yval), y);
        //let ay = ayrad * 180.0 / Float.pi;

        return ayrad;
    }

    static func xOrient(x:Float, y:Float, z:Float) -> Float
    {
        let xval = z * z + y * y;
        let axrad = atan2(sqrt(xval), x);
        //let ax = axrad * 180.0 / Float.pi;

        return axrad;
    }
}

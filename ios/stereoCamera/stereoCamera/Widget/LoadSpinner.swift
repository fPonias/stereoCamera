//
//  LoadSpinner.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/6/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class LoadSpinner : UIView
{
    private let glassesImg = UIImage(named: "glasses")
    private var glassesImgScaled:UIImage? = nil
    private let glowImg = UIImage(named: "glasses_glow")
    private var glowImgScaled:UIImage? = nil

    private var lastRect:CGRect? = nil
    private var glassesDrawPoint = CGPoint(x: 0.0, y: 0.0)
    private var glassesScale:CGFloat = 1.0

    private var glowTransparency:CGFloat = 0.0
    private let glowStep:CGFloat = 5.0 / 256.0
    private let glowMax:CGFloat = 1.0
    private let glowMin:CGFloat = 0.0
    private var glowBrighter:CGFloat = 1.0

    private func drawGlow()
    {
        //glow.setAlpha(glowTransparency);
    }

    private let circleCount = 10
    private var circleBigRadius = CGFloat(35.0)
    private var circleSmallRadius = CGFloat(3.0)
    private var circleStartIdx = 0
    private let circleAlphaStep = CGFloat(30.0 / 255.0)
    private let circleRotateStart = 0.0
    private let circleRotateStep = 5.0
    private let circleStepDelay = 0.10
    
    private var centerx = CGFloat(0.0)
    private var centery = CGFloat(0.0)
    private var doublePi = CGFloat(Double.pi * 2.0)

    private func drawCircles()
    {
        var rotateM = Array(repeating: Array(repeating: CGFloat(0), count: 2), count: 2)
        var center:[CGFloat]  = [0.0, circleBigRadius]
        let angle =  -doublePi / CGFloat(circleCount)
        
        rotateM[0][0] = CGFloat(cos(angle))
        rotateM[0][1] = CGFloat(sin(angle))
        rotateM[1][0] = -rotateM[0][1]
        rotateM[1][1] = rotateM[0][0]

        var alpha = CGFloat(0.0);
        center = rotate(center: center, rotateM: rotateM, multiplier: circleStartIdx)

        for _ in 1 ... circleCount
        {
            UIColor(displayP3Red: 0.0, green: 0.0, blue: 0.0, alpha: alpha).setFill()
            let arcCenter = CGPoint(x: center[0] + centerx, y: center[1] + centery)
            let path = UIBezierPath(arcCenter: arcCenter, radius: circleSmallRadius, startAngle: 0.0, endAngle: 360.0, clockwise: true)
            path.fill()

            alpha = min(1.0, alpha + circleAlphaStep);
            center = rotate(center: center, rotateM: rotateM)
        }
    }
    
    private func rotate(center:[CGFloat], rotateM:[[CGFloat]], multiplier:Int = 0) -> [CGFloat]
    {
        //manual matrix multiplication
        var newCenter:[CGFloat] = [center[0], center[1]]
        var newCenter2:[CGFloat] = [0.0, 0.0]
        
        for _ in 0 ... multiplier
        {
            newCenter2[0] = rotateM[0][0] * newCenter[0] + rotateM[0][1] * newCenter[1]
            newCenter2[1] = rotateM[1][0] * newCenter[0] + rotateM[1][1] * newCenter[1]
            newCenter[0] = newCenter2[0]
            newCenter[1] = newCenter2[1]
        }
        
        return newCenter
    }
    
    override func draw(_ rect: CGRect)
    {
        if (lastRect == nil || lastRect?.width != rect.width || lastRect?.height != rect.height)
        {
            reload(rect: rect)
        }
        
        updateCircles()
        drawCircles()
        
        glassesImgScaled?.draw(at: glassesDrawPoint)
        updateGlow()
        glowImgScaled?.draw(at: glassesDrawPoint, blendMode: CGBlendMode.normal, alpha: glowTransparency)
        
        drawDone = true
    }
    
    private func updateCircles()
    {
        let now = Date().timeIntervalSince1970
        let diff = now - lastCircleDraw
        if (diff > circleStepDelay)
        {
            circleStartIdx = (circleStartIdx + 1) % circleCount
            lastCircleDraw = now
        }
    }
    
    private func updateGlow()
    {
        glowTransparency += glowBrighter * glowStep
        
        if (glowBrighter > 0 && glowTransparency > glowMax)
        {
            glowBrighter = -1.0
            glowTransparency = glowMax
        }
        else if (glowBrighter < 0 && glowTransparency < glowMin)
        {
            glowBrighter = 1.0
            glowTransparency = glowMin
        }
    }
    
    private func reload(rect: CGRect)
    {
        centerx = rect.width / 2.0
        centery = rect.height / 2.0
        
        let dim = min(rect.width, rect.height) * 0.7
        let marginx = (rect.width - dim) * 0.5
        let marginy = (rect.height - dim) * 0.5
        glassesScale = dim / glassesImg!.size.width
        glassesDrawPoint = CGPoint(x: marginx, y: marginy)
        
        let sz = __CGSizeApplyAffineTransform(glassesImg!.size, CGAffineTransform(scaleX: glassesScale, y: glassesScale))
        UIGraphicsBeginImageContextWithOptions(sz, false, 0.0)
        glassesImg?.draw(in: CGRect(origin: CGPoint.zero, size: sz))
        glassesImgScaled = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        UIGraphicsBeginImageContextWithOptions(sz, false, 0.0)
        glowImg?.draw(in: CGRect(origin: CGPoint.zero, size: sz))
        glowImgScaled = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        circleBigRadius = (min(rect.width, rect.height) * 0.9) * 0.5
        circleSmallRadius = circleBigRadius * 0.1
    }
    
    private var isRunning = false
    private var drawDone = true
    private var lastCircleDraw:Double = 0
    
    func start()
    {
        if (isRunning)
            { return }
    
        let t = Thread(block: {
        [unowned self] in
            self.run()
        })
        t.start()
    }
    
    func run()
    {
        isRunning = true
        
        while (isRunning)
        {
            if (drawDone)
            {
                drawDone = false
                
                DispatchQueue.main.async{
                [unowned self] in
                    self.setNeedsDisplay()
                }
            }
            
            usleep(10000);
        }
    }
    
    func stop()
    {
        isRunning = false
    }
}

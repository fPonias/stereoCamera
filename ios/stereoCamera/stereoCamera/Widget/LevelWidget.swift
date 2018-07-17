//
//  LevelWidget.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/16/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class LevelWidget : UIView
{
    required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
        loadImages()
    }
    
    override init(frame: CGRect)
    {
        super.init(frame: frame)
        self.rect = frame
        loadImages()
    }
    
    private var compass = UIImageView()
    private var arrow1 = UIImageView()
    private var arrow2 = UIImageView()
    private func loadImages()
    {
        compass.frame = self.rect
        compass.image = UIImage(named: "compass")
        addSubview(compass)
        
        arrow1.frame = self.rect
        arrow1.image = UIImage.init(named: "compass_arrow")
        addSubview(arrow1)
        
        arrow2.frame = self.rect
        arrow2.image = UIImage.init(named: "compass_arrow2")
        addSubview(arrow2)
    }
    
    private func getRotatedImage(rotation: CGFloat, image:CIImage) -> UIImage
    {
        let tx = rect.width / 2
        let ty = rect.height / 2
        let scale = CGFloat(rect.width / image.extent.width)
        let ex = image.extent.width / 2
        
        var transform = CGAffineTransform(translationX: -ex, y: -ex)
        transform = transform.rotated(by: rotation)
        transform = transform.translatedBy(x: ex, y: ex)
        //var transform = CGAffineTransform(scaleX: scale, y: scale)
        let img = image.transformed(by: transform)
        return UIImage(ciImage: img)
    }
    
    private func getRotatedRect(rotation: Float) -> CGRect
    {
        let w = Float(rect.width)
        let z = w / (cos(rotation) + sin(rotation))
        let x = w - (z * cos(rotation))
        let y = w - (z * sin(rotation))
        
        let whole = min(abs(x), abs(y))
        let half = CGFloat(whole * 0.5)
        
        let scale = (w > z) ? abs(w / z) : abs(z / w)
        let retw = CGFloat(w * scale)
        
        return CGRect(x:-half, y:-half, width: retw, height: retw)
    }
    
    private var rect = CGRect(x: 0, y: 0, width: 50, height: 50)
    
    private var arrow1Rot:UIImage = UIImage()
    private var arrow1Rect = CGRect(x: 0, y: 0, width: 10, height: 10)
    private var _rotation1:Float = 0
    var rotation1:Float
    {
        get { return _rotation1 }
        set
        {
            _rotation1 = newValue
            
            DispatchQueue.main.async {
            [ unowned self ] in
                let transform = CGAffineTransform(rotationAngle: CGFloat(self._rotation1))
                self.arrow1.transform = transform
            }
        }
    }
    
    private var arrow2Rot:UIImage = UIImage()
    private var _rotation2:Float = 0
    var rotation2:Float
    {
        get { return _rotation2 }
        set
        {
            _rotation2 = newValue
            
            DispatchQueue.main.async {
            [ unowned self ] in
                let transform = CGAffineTransform(rotationAngle: CGFloat(self._rotation2))
                self.arrow2.transform = transform
            }
        }
    }
}

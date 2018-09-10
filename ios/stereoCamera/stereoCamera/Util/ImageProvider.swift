//
//  File.swift
//  stereoCamera
//
//  Created by hallmarklabs on 9/6/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class ImageProvider : UIActivityItemProvider
{
    enum ExportType : Int
    {
        case COPY = 1,
        ROTATE_TO_PORTRAIT,
        SQUARE,
        ROTATE_TO_SQUARE
    }

    var target:UIImage
    var type:ExportType

    init(placeholderItem:UIImage, type: ExportType = .COPY)
    {
        self.target = placeholderItem
        self.type = type
        super.init(placeholderItem: placeholderItem)
    }

    override var item: Any
    {
        get
        {
            print ("for activity type " + activityType.debugDescription)
            
            if (type == .COPY)
                { return target }
            
            let sz:CGSize?
            switch(type)
            {
                case .ROTATE_TO_PORTRAIT:
                    sz = CGSize(width: 1080, height: 1350)
                case .ROTATE_TO_SQUARE, .SQUARE:
                    sz = CGSize(width: 1080, height: 1080)
                default:
                    sz = nil
            }
            
            guard(sz != nil) else { return target }
            let (cgImage, outContext) = prepVars(outDims: sz!)
            guard(cgImage != nil) else { return target }
            guard(outContext != nil) else { return target }
            
            switch(type)
            {
                case .ROTATE_TO_PORTRAIT, .ROTATE_TO_SQUARE:
                    return getRotatedImage(size: sz!, image: cgImage!, context: outContext!)
                case .SQUARE:
                    return getSquaredImage(size: sz!, image: cgImage!, context: outContext!)
                default:
                    return target
            }
        }
    }
    
    private func prepVars(outDims:CGSize) -> (CGImage?, CGContext?)
    {
        print ("converting export image")
        //convert the preview image to a mutable image
        let ciImag = CIImage(image: target)
        guard ciImag != nil else { return (nil, nil) }
        let ciImage = ciImag!
        
        let context = CIContext(options: nil)
        let cgImag = context.createCGImage(ciImage, from: ciImage.extent)
        guard cgImag != nil else { return (nil, nil) }
        let cgImage = cgImag!

        //setup the drawing context and match the source image encoding parameters
        let outContex = CGContext.init(data: nil, width: Int(outDims.width), height: Int(outDims.height), bitsPerComponent: cgImage.bitsPerComponent, bytesPerRow: cgImage.bitsPerPixel * Int(outDims.width), space: cgImage.colorSpace!, bitmapInfo: cgImage.bitmapInfo.rawValue)
        
        guard outContex != nil else { return (nil, nil) }
        let outContext = outContex!
        
        //draw a white background
        let outRect = CGRect(x: 0, y: 0, width: outDims.width, height: outDims.height)
        outContext.setFillColor(UIColor.white.cgColor)
        outContext.fill(outRect)
        
        return (cgImage, outContext)
    }
    
    func getRotatedImage(size outDims:CGSize, image:CGImage, context:CGContext) -> UIImage
    {
        //draw the source image to scale and rotate to Instagram image sizing parameters
        //guess n' check matrix rotation and translation
        let scaledHeight = outDims.height / CGFloat(image.width) * CGFloat(image.height)
        context.rotate(by: -CGFloat.pi / 2.0)
        context.translateBy(x: -outDims.height, y: (outDims.width - scaledHeight) * 0.5)
        let imgRect = CGRect(x: 0, y: 0, width: outDims.height, height: scaledHeight)
        context.draw(image, in: imgRect)
        
        let newImg = context.makeImage()
        let ret = UIImage(cgImage: newImg!)

        print ("converted!")
        return ret
    }
    
    func getSquaredImage(size outDims:CGSize, image:CGImage, context:CGContext) -> UIImage
    {
        let halfHeight = outDims.height * 0.5
        var imgRect = CGRect(x: 0, y: 0, width: outDims.width, height: halfHeight)
        context.draw(image, in: imgRect)
        imgRect = CGRect(x: 0, y: halfHeight, width: outDims.width, height: halfHeight)
        context.draw(image, in: imgRect)
        
        let newImg = context.makeImage()
        let ret = UIImage(cgImage: newImg!)

        print ("converted!")
        return ret
    }
}

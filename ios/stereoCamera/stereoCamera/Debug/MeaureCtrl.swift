//
//  MeaureCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/18/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class MeasureCtrl : UIViewController
{
    var leftPixels:CVPixelBuffer?
    var rightPixels:CVPixelBuffer?
    var zoom:Float = 1.64
    
    @IBOutlet weak var summaryView: MeasureCtrlScroller!
    
    private var leftData:[Float]?
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    @IBAction func histogramBtn(_ sender: Any)
    {
        guard let leftPixels = leftPixels,
              let rightPixels = rightPixels
        else { return }
        
        
        let leftImg = renderImage(buffer: leftPixels, zoom: 1.0, offset: 0)
        guard let leftImg = leftImg else { return }
        
        
        let leftEditData = ImageEditorData(origData: leftPixels, zoom: 1.0, rotation: 0, offset: CGPoint(x: 0.0, y: 0.0))
        let histogram = Histogram()
        histogram.setPixels(pixels: leftPixels)
        histogram.setMargins(Histogram.TextureMargin(left: leftEditData.margins.left, top: leftEditData.margins.top, right: leftEditData.margins.right, bottom: leftEditData.margins.bottom))
                
        let histogram2 = Histogram()
        histogram2.setPixels(pixels: rightPixels)
        
        summaryView.reset()
        var results = [Float]()
        for offset in stride(from: -200, to: 200, by: 40) {
            let rightEditData = ImageEditorData(origData: rightPixels, zoom: 1.64, rotation: 0, offset: CGPoint(x: 0.0, y: CGFloat(offset)))
            histogram2.setMargins(Histogram.TextureMargin(left: rightEditData.margins.left, top: rightEditData.margins.top, right: rightEditData.margins.right, bottom: rightEditData.margins.bottom)
            )
            histogram2.setZoom(Float(rightEditData.zoom))
            
            let (leftData, rightData) = compareHistograms(histogram1: histogram, histogram2: histogram2)

            
            let img = renderImage(buffer: rightPixels, zoom: 1.64, offset: CGFloat(offset))
            guard let img = img else { continue }
            
            summaryView.append(leftPixels: leftImg, rightPixels: img, data: [leftData, rightData], size: CGSize(width: summaryView.frame.height / 3.0 * 1.3, height: summaryView.frame.height))
            
            results.append(diff(orig: leftData, zoomed: rightData, ratio: 1.0))
        }
    }
    
    private func populateHistogram(histogram: Histogram, data:ImageEditorData) {
        let movedMargin = Histogram.TextureMargin(left: data.margins.left, top: data.margins.top,
                                          right: data.margins.right, bottom: data.margins.bottom)
        histogram.setMargins(movedMargin)
        histogram.setZoom(Float(data.zoom))
    }
    
    private func compareHistograms(histogram1: Histogram, histogram2: Histogram) -> ([Float], [Float]) {
        guard let data2 = histogram2.calculate(),
              let data1 = histogram1.calculate()
        else { return ([Float](), [Float]()) }
        
        var data1Count:Int32 = 0
        var data2Count:Int32 = 0
        for i in 0 ..< data1.count {
            data1Count += data1[i]
            data2Count += data2[i]
        }
        
        let ratio = Float(data1Count) / Float(data2Count)
        
        var data1f:[Float] = Array()
        for data1Item in data1 {
            data1f.append(Float(data1Item))
        }
        
        var data2f:[Float] = Array()
        for data2Item in data2 {
            let data2Val = Float(data2Item) * ratio
            data2f.append(data2Val)
        }
        
        return (data1f, data2f)
    }
    
    private func renderImage(buffer: CVPixelBuffer, zoom: CGFloat, offset: CGFloat) -> UIImage?
    {
        var img:CIImage? = CIImage(cvPixelBuffer: buffer)
        
        let offset = CGPoint(x: CGFloat(0), y: CGFloat(offset))
        let squareFilter = SquareFilter(orientation: .DEG_0, zoom: zoom, offset: offset)
        img = squareFilter.update(img!)
        
        
        guard let imgout = img else { return nil }
        
        let ctx = CIContext()
        var outBuf:CVPixelBuffer?
        CVPixelBufferCreate(kCFAllocatorDefault, Int(2160), Int(2160), kCVPixelFormatType_32BGRA, [kCVPixelBufferMetalCompatibilityKey: true] as CFDictionary, &outBuf)
        
        guard let outBufConst = outBuf else { return nil }
        
        ctx.render(imgout, to: outBufConst)
        let ciImage = CIImage(cvPixelBuffer: outBufConst)
        let uiImage = UIImage(ciImage: ciImage)
        
        return uiImage
    }
    
    func diff(orig:[Float], zoomed:[Float], ratio:Float) -> Float {
        var ret = Float(0.0)
        
        for i in 0 ..< orig.count {
            let d = Float(orig[i]) - ratio * Float(zoomed[i])
            ret += d * d
        }
        
        return ret
    }
}

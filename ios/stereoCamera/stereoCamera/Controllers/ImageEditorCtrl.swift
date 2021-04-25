//
//  ImageEditorCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/7/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit
import VideoToolbox

class ImageEditorCtrl : UIViewController
{
    @IBOutlet weak var leftPreview: UIImageView!
    @IBOutlet weak var rightPreview: UIImageView!
    @IBOutlet weak var zoomSlider: UISlider!
    
    var leftData:ImageEditorData?
    var rightData:ImageEditorData?
    
    static func initFromStoryboard() -> ImageEditorCtrl
    {
        let sb = UIStoryboard.init(name: "Main", bundle: nil)
        let ret = sb.instantiateViewController(withIdentifier: "ImageEditor")
    
        return ret as! ImageEditorCtrl
    }
    
    override func viewDidLoad() {
        zoomSlider.minimumValue = -Float.pi / 2.0
        zoomSlider.maximumValue = Float.pi / 2.0
        zoomSlider.value = 0
        zoomSlider.addTarget(self, action: #selector(sliderChanged), for: .valueChanged)
    }
    
    @objc public func sliderChanged(slider: UISlider) {
        guard let ldata = leftData else { return }
        adjust(data: ldata, preview: leftPreview)
        
        guard let rdata = rightData else { return }
        adjust(data: rdata, preview: rightPreview)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        /*
        if let data = leftData, let preview = leftPreview {
            let _ = CVPixelBufferCreate(kCFAllocatorDefault, Int(preview.frame.width), Int(preview.frame.height), kCVPixelFormatType_32BGRA, nil, &data.previewBuf)
            guard data.previewBuf != nil else { return }
        }
        
        if let data = rightData, let preview = rightPreview {
            let _ = CVPixelBufferCreate(kCFAllocatorDefault, Int(preview.frame.width), Int(preview.frame.height), kCVPixelFormatType_32BGRA, nil, &data.previewBuf)
            guard data.previewBuf != nil else { return }
        }*/
        
        update()
    }
    
    private func update()
    {
        update2(data: leftData, preview: leftPreview)
        update2(data: rightData, preview: rightPreview)
    }
    
    private func update2(data:ImageEditorData?, preview:UIImageView?) {
        guard let data = data else { return }
        adjust(data: data, preview: preview)
    }
    
    private func adjust(data:ImageEditorData, preview:UIImageView?)
    {
        guard let preview = preview,
              let buffer = data.origData
        else { return }
        
        var img:CIImage? = CIImage(cvPixelBuffer: buffer)
        let squareFilter = SquareFilter(orientation: .DEG_0, zoom: 1.0)
        img = squareFilter.update(img!)
        
        let rotateFIlter = RotateFilter(rotation: zoomSlider.value, dimension: 2160)
        img = rotateFIlter.update(img!)
        
        guard let imgout = img else { return }
        
        let ctx = CIContext()
        let sz = imgout.extent
        var outBuf:CVPixelBuffer?
        CVPixelBufferCreate(kCFAllocatorDefault, Int(2160), Int(2160), kCVPixelFormatType_32BGRA, [kCVPixelBufferMetalCompatibilityKey: true] as CFDictionary, &outBuf)
        
        guard let outBufConst = outBuf else { return }
        
        ctx.render(imgout, to: outBufConst)
        let ciImage = CIImage(cvPixelBuffer: outBufConst)
        let uiImage = UIImage(ciImage: ciImage)
        preview.image = uiImage
    }
    
    private let saver = Files.instance
    private var outBuffer:CVPixelBuffer?
    
    
}

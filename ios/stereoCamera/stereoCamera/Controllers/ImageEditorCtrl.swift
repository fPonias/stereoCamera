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
        zoomSlider.minimumValue = 0.0
        zoomSlider.maximumValue = 1.0
        zoomSlider.value = 1.0
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
    {/*
        guard let preview = preview,
              let buf = data.previewBuf
        else { return }
        
        var img = data.tmpImg
        
        let filter = ScaleWidget(type: .RELATIVE)
        filter.value = Float(preview.frame.width)
        img = filter.update(img!)
        
        guard let pimg = img else { return }
        
        let ctx = CIContext()
        ctx.render(pimg, to: buf)
        let ciImage = CIImage(cvPixelBuffer: buf)
        let uiImage = UIImage(ciImage: ciImage)
        preview.image = uiImage*/
    }
    
    private let saver = Files.instance
    private var outBuffer:CVPixelBuffer?
    
    
}

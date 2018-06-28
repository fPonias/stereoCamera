//
//  GalleryBtn.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/24/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit
import Photos

class GalleryBtn : UIButton
{
    var files = [PHAsset]()
    
    override init(frame: CGRect)
    {
        super.init(frame: frame)
        viewDidLoad()
    }
    
    required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
        viewDidLoad()
    }
    
    func viewDidLoad()
    {
        files = Files.getGalleryFiles()
        if (files.count > 0)
        {
            let asset = files[files.count - 1]
            let image = Files.assetToImage(asset, asThumbnail: true)
            setImage(image, for: .normal)
        }
    }
}

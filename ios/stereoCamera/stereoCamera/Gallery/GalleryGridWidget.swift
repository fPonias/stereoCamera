//
//  GalleryGridWidget.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/20/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit
import Photos

class GalleryGridWidget: UICollectionViewCell
{
    @IBOutlet weak var thumbnail: UIImageView!
    @IBOutlet weak var playIcon: UIImageView!
    
    
    
    func displayContent(asset: PHAsset)
    {
        Files.instance.assetToImage(asset, asThumbnail: true, completed: {[weak self] img in
            guard let self = self,
                  let image = img
            else { return }
            
            self.playIcon.isHidden = asset.mediaType != .video
            self.selectedOverlay.isHidden = !self.isSelected
            
            self.scaleImage(image: image)
            self.thumbnail.image = image
        })
    }
    
    private func scaleImage(image:UIImage) {
        let w = frame.width
        let h = frame.height
        let frratio = w / h
        let iw = image.size.width
        let ih = image.size.height
        
        guard iw > 0 && ih > 0 else { return }
        
        let imgratio = iw/ih
        
        if (frratio > imgratio) {
            let scale = ih / h
            let scaleW = iw / scale
            let margin = (w - scaleW) / 2.0
            
            thumbnail.frame = CGRect(x: margin, y: 0, width: scaleW, height: h)
        } else {
            let scale = iw / w
            let scaleH = ih / scale
            let margin = (scaleH - h) / 2.0
            
            thumbnail.frame = CGRect(x: 0, y: margin, width: w, height: scaleH)
        }
        
        setNeedsLayout()
    }
    
    override var isSelected: Bool {
        get { return super.isSelected }
        set {
            super.isSelected = newValue
            selectedOverlay.isHidden = !newValue
        }
    }
    
    @IBOutlet weak var selectedOverlay: UIView!
}

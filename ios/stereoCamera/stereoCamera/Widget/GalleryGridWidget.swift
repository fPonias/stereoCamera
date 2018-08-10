//
//  GalleryGridWidget.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/20/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class GalleryGridWidget: UICollectionViewCell
{
    @IBOutlet weak var thumbnail: UIImageView!
    
    func displayContent(image: UIImage)
    {
        thumbnail.image = image
    }
    
    private var _highlighted = false
    override var isHighlighted:Bool
    {
        get { return _highlighted }
        set
        {
            _highlighted = newValue
            selectedOverlay.isHidden = !_highlighted
        }
    }
    
    @IBOutlet weak var selectedOverlay: UIView!
}

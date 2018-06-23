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
}

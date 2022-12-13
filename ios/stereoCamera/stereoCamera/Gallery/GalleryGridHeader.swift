//
//  GalleryGridHeader.swift
//  stereoCamera
//
//  Created by hallmarklabs on 9/5/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class GalleryGridHeader : UICollectionReusableView
{
    @IBOutlet weak var label: UILabel!
    
    var title:String?
    {
        get { return label.text }
        set { label.text = newValue }
    }
}

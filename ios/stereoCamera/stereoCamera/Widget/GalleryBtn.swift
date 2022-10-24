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

class GalleryBtn : UIImageView, FilesDelegate //I'd make this a UIButton but the image won't display properly
{
    func onNewFile(asset: PHAsset) {
        Files.instance.assetToImage(asset, completed: { img in
            DispatchQueue.main.async { [weak self] in
                self?.image = img
            }
        })
    }
    
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
        //update()
        
        let gest = UITapGestureRecognizer(target: self, action: #selector(GalleryBtn.onTap))
        addGestureRecognizer(gest)
        
        Files.instance.addDelegate(self)
        if let nc = AppDelegate.instance?.window?.rootViewController, nc is UINavigationController {
            navCtrl = (nc as! UINavigationController)
        }
        
    }
    
    //hack.  adding this via the storyboard is proving difficult
    @objc func onTap()
    {
/*        if (navCtrl == nil)
        {
            print("please use setNavigationController() before opening the gallery view with GalleryBtn")
            return
        }
        
        let ctrl = GalleryGridCtrl.initFromStoryboard()
        navCtrl?.pushViewController(ctrl, animated: true)*/
    }
    
    private weak var navCtrl:UINavigationController? = nil
    
    func update()
    {
        files = Files.instance.getGalleryFiles()
        if (files.count > 0)
        {
            let asset = files[files.count - 1]
            update(with: asset)
        }
    }
    
    func update(with: PHAsset)
    {
        let image = Files.instance.assetToImage(with, completed: {[weak self] image in
            self?.image = image
        })
    }
}

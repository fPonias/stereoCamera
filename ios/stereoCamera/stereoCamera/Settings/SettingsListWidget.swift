//
//  SettingsListWidget.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/31/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class SettingsListWidget : UICollectionViewCell
{
    override func didMoveToSuperview() {
        let regoc = UITapGestureRecognizer(target: self, action: #selector(tapped))
        regoc.numberOfTapsRequired = 1
        rootView.addGestureRecognizer(regoc)
    }
    
    private var _id:Int = 0
    var id:Int
    {
        get { return _id }
    }

    func setData(id:Int, title:String, selected:Bool)
    {
        _id = id
        label.text = title
        checkbox.image = (selected) ? UIImage(named: "checkbox-checked") : UIImage(named: "checkbox-unchecked")
    }
    
    private var listener:Optional<(Int) -> Void> = nil
    func setTapListener( _ listener:@escaping (Int) -> Void)
    {
        self.listener = listener
    }

    @IBOutlet weak var rootView: UIView!
    @IBOutlet weak var label: UILabel!
    @IBOutlet weak var checkbox: UIImageView!
    
    @objc func tapped()
    {
        listener?(_id)
    }
}

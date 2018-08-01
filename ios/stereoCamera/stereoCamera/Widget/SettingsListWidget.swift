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
    private var _id:Int = 0
    var id:Int
    {
        get { return _id }
    }

    func setData(id:Int, title:String)
    {
        _id = id
        button.setTitle(title, for: .normal)
    }
    
    private var listener:Optional<(Int) -> Void> = nil
    func setTapListener( _ listener:@escaping (Int) -> Void)
    {
        self.listener = listener
    }

    @IBOutlet weak var button: UIButton!
    
    @IBAction func tapped(_ sender: Any)
    {
        if (listener != nil)
        {
            listener!(_id)
        }
    }
}

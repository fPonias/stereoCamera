//
//  DesignableTextField.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/29/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

extension UITextField
{
    func addDoneButtonToKeyboard(target: Any, myAction:Selector?)
    {
        let doneToolbar: UIToolbar = UIToolbar(frame: CGRect(x: 0, y: 0, width: 300, height: 40))
        doneToolbar.barStyle = UIBarStyle.default

        let flexSpace = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.flexibleSpace, target: nil, action: nil)
        let done: UIBarButtonItem = UIBarButtonItem(title: "Done", style: UIBarButtonItemStyle.done, target: target, action: myAction)

        var items = [UIBarButtonItem]()
        items.append(flexSpace)
        items.append(done)

        doneToolbar.items = items
        doneToolbar.sizeToFit()

        self.inputAccessoryView = doneToolbar
    }
}

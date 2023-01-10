//
//  FilterSelectWidget.swift
//  stereoCamera
//
//  Created by Cody Munger on 1/3/23.
//  Copyright © 2023 cody. All rights reserved.
//

import Foundation
import UIKit

class FilterSelectWidget : UICollectionViewCell {
    private var _id:Int = 0
    var id:Int {
        get { return _id }
        set { _id = newValue }
    }
    
    func setData(id: Int, title: String, selected: Bool) {
        _id = id
        label.text = title
        self.isSelected = selected
        
        updateColors()
    }
    
    private func updateColors() {
        if (_enabled) {
            checkbox.image = (isSelected) ? UIImage(named: "checkbox-checked") : UIImage(named: "checkbox-unchecked")
            label.textColor = UIColor.black
        } else {
            checkbox.image = (isSelected) ? UIImage(named: "checkbox-checked-grey") : UIImage(named: "checkbox-unchecked-grey")
            label.textColor = UIColor(red: 0.863, green: 0.863, blue: 0.863, alpha: 1.0)
        }
    }
    
    @IBOutlet weak var label: UILabel!
    @IBOutlet weak var checkbox: UIImageView!
    @IBOutlet weak var rootView: UIView!
    
    private var _enabled:Bool = true
    var enabled:Bool {
        get { return _enabled }
        set {
            if (newValue == _enabled) { return }
            
            _enabled = newValue
            updateColors()
        }
    }
    
    @objc func tapped(_ sender: Any) {
        if (!_enabled) { return }
        
        onTapped?(self)
    }
    
    var onTapped:((FilterSelectWidget) -> Void)? = nil
    
    override func didMoveToSuperview() {
        let regoc = UITapGestureRecognizer(target: self, action: #selector(tapped))
        regoc.numberOfTapsRequired = 1
        rootView.addGestureRecognizer(regoc)
    }
}

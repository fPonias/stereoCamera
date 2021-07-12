//
//  PopupButtonPicker.swift
//  stereoCamera
//
//  Created by Cody Munger on 5/8/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class PopupButtonPicker : PopupButton {
    public var pickedItem:PopupButtonModalPickerItem? {
        get {
            guard let modal = _modal else { return nil }
            guard let item = modal.selectedItem as? PopupButtonModalPickerItem else { return nil }
            return item
        }
        set(value) {
            guard let modal = _modal else { return }
            guard let item = value else { return }
            modal.selectedItem = item as Any
            
            guard let selItem = modal.selectedItem as? PopupButtonModalPickerItem else { return }
            let title = delegate?.getTitle(selected: selItem) ?? titleLabel?.text ?? ""
            setTitle(title, for: .normal)
            
            setNeedsDisplay()
        }
    }
    
    public override func createModal() -> PopupButtonModal {
        if let modal = _modal {
            return modal
        }
        
        _modal = PopupButtonModalPicker(parent: self)
        return modal!
    }
}

class PopupButtonModalPicker : PopupButtonModal {
    override init(parent: PopupButton) {
        super.init(parent:parent)
        setup()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setup() {
        backgroundColor = UIColor.gray
        origin = frame.origin
    }
    
    private var _pickerViewArray:[PopupButtonModalPickerItem] = Array()
    public var valueList:[PopupButtonModalPickerItem] {
        get { return _pickerViewArray }
        set(value) {
            _pickerViewArray = value
            
            if let parent = parent as? PopupButtonPicker,
               !valueList.isEmpty {
                parent.pickedItem = _pickerViewArray[0]
            }
            
            updateSubviews()
            setNeedsLayout()
        }
    }
    
    private var buttonMap:[UIButton:PopupButtonModalPickerItem] = Dictionary()
    private var buttonIndexMap:[UIButton:Int] = Dictionary()
    
    func updateSubviews() {
        for child in subviews {
            willRemoveSubview(child)
            child.removeFromSuperview()
        }
        
        buttonMap.removeAll()
        buttonIndexMap.removeAll()
        
        for i in 0 ..< _pickerViewArray.count {
            let btn = UIButton()
            buttonMap[btn] = _pickerViewArray[i]
            buttonIndexMap[btn] = i
            
            btn.setTitle(_pickerViewArray[i].text, for: .normal)
            btn.titleLabel?.text = _pickerViewArray[i].text
            btn.addTarget(self, action: #selector(onTapped), for: .primaryActionTriggered)
            
            addSubview(btn)
        }
    }
    
    override func layoutSubviews() {
        var offset = 0
        
        for i in 0 ..< subviews.count {
            let i2 = subviews.count - 1 - i
            let sz = _pickerViewArray[i2].text.count
            let width = max(50, sz * 8)
            offset += 10
            
            let frame = CGRect(x: offset, y: 10, width: width, height: 20)
            let btn = subviews[i2] as! UIButton
            btn.frame = frame
            
            offset += width
        }
        
        frame.size.width = CGFloat(offset + 10)
        frame.size.height = 40.0
        frame.origin.x = origin.x - frame.size.width
    }
    
    @objc func onTapped(sender: AnyObject) {
        guard let button = sender as? UIButton,
              let parent = parent as? PopupButtonPicker
        else { return }
        guard let index = buttonIndexMap[button] else { return }
        
        let val = _pickerViewArray[index]
        selectedItem = val
        parent.pickedItem = val
        parent.delegate?.itemSelected(parent, value: val)
        parent.close()
    }
    
    override public var selectedItem:Any {
        get { return super.selectedItem }
        set(value) {
            super.selectedItem = value
            
            if let val = value as? PopupButtonModalPickerItem {
                for sub in subviews {
                    if let btn = sub as? UIButton {
                        btn.isSelected = buttonMap[btn]?.text == val.text ? true : false
                    }
                }
            }
            
            setNeedsDisplay()
        }
    }
}

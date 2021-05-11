//
//  CameraTypePicker.swift
//  stereoCamera
//
//  Created by Cody Munger on 5/6/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class CameraTypePicker : UIPickerView, UIPickerViewDelegate, UIPickerViewDataSource
{
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        
        setup()
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        setup()
    }
    
    private func setup() {
        delegate = self
        dataSource = self
        
        showsSelectionIndicator = false
        backgroundColor = UIColor.clear
    }
    
    override func didMoveToSuperview() {
        guard let superview = superview else { return }
        let parentFrm = superview.frame
        
        frame = CGRect(x: 0, y: -36, width: parentFrm.width, height: parentFrm.height + 92)
    }
    
    private var _pickerViewArray:[CameraTypeItem] = Array()
    public var valueList:[CameraTypeItem] {
        get { return _pickerViewArray }
        set(value) {
            _pickerViewArray = value
            setNeedsLayout()
        }
    }
    
    func numberOfComponents(in: UIPickerView) -> Int {
        return 1
    }
    
    func pickerView(_ in: UIPickerView, numberOfRowsInComponent: Int) -> Int {
        return _pickerViewArray.count
    }
    
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        guard !_pickerViewArray.isEmpty && row < _pickerViewArray.count else { return }
        listener?.onSelected(self, index: row, value: _pickerViewArray[row])
    }
    
    func pickerView(_ pickerView: UIPickerView, widthForComponent component: Int) -> CGFloat {
        return 100
    }
    
    func pickerView(_ pickerView: UIPickerView, rowHeightForComponent component: Int) -> CGFloat {
        return 40
    }
    
    func pickerView(_ pickerView: UIPickerView, viewForRow row: Int, forComponent component: Int, reusing view: UIView?) -> UIView {
        let rect = CGRect(x: 0, y: 0, width: 100, height: 20)
        let label = UILabel(frame: rect)
                
        label.text = _pickerViewArray[row].title
        label.font = UIFont.systemFont(ofSize: 14.0, weight: .bold)
        label.textColor = UIColor.white
        label.textAlignment = .center
        label.numberOfLines = 1
        label.lineBreakMode = .byClipping
        label.backgroundColor = UIColor.clear
        label.clipsToBounds = true
        
        return label
    }
    
    public var listener:CameraTypePickerDelegate?
    
    public var selectedItem:CameraTypeItem {
        get {
            let idx = selectedRow(inComponent: 0)
            let item = _pickerViewArray[idx]
            return item
        }
        set(value) {
            var idx:Int? = nil
            for i in 0 ..< _pickerViewArray.count {
                if _pickerViewArray[i].title == value.title && _pickerViewArray[i].type == value.type {
                    idx = i
                    break
                }
            }
            guard let index = idx else { return }
            selectRow(index, inComponent: 0, animated: true)
        }
    }
    
    public var selectedIndex:Int {
        get { return selectedRow(inComponent: 0)}
        set(value) { selectRow(value, inComponent: 0, animated: true)}
    }
}

struct CameraTypeItem
{
    enum CameraType {
        case CAMERA
        case VIDEO
    }
    
    let title:String
    let type:CameraType
}

protocol CameraTypePickerDelegate
{
    func onSelected(_ picker:CameraTypePicker, index:Int, value:CameraTypeItem)
}

class HorizontalCameraTypePicker : CameraTypePicker
{
    override func didMoveToSuperview() {
        guard let superview = superview else { return }
        let parentFrm = superview.frame
        
        var rotate = CGAffineTransform(rotationAngle: CGFloat(-(Float.pi)) / 2.0)
        //rotate = rotate.scaledBy(x: 0.25, y: 2.0)
        transform = rotate
        frame = CGRect(x: -150, y: 0, width: parentFrm.width + 300, height: 46)
    }
    
    override func pickerView(_ pickerView: UIPickerView, viewForRow row: Int, forComponent component: Int, reusing view: UIView?) -> UIView {
        let label = super.pickerView(pickerView, viewForRow: row, forComponent: component, reusing: view)
        
        let rotate = CGAffineTransform(rotationAngle: CGFloat.pi / 2.0)
        //rotate.scaledBy(x: 0.25, y: 2.0)
        label.transform = rotate
        
        return label
    }
    
    override func pickerView(_ pickerView: UIPickerView, widthForComponent component: Int) -> CGFloat {
        return 40
    }
    
    override func pickerView(_ pickerView: UIPickerView, rowHeightForComponent component: Int) -> CGFloat {
        return 160
    }
}

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
        
    }
    
    override func didMoveToSuperview() {
        guard let superview = superview else { return }
        let parentFrm = superview.frame
        
        showsSelectionIndicator = false
        backgroundColor = UIColor.clear
        var rotate = CGAffineTransform(rotationAngle: CGFloat(-(Float.pi)) / 2.0)
        //rotate = rotate.scaledBy(x: 0.25, y: 2.0)
        transform = rotate
        frame = CGRect(x: -150, y: 0, width: parentFrm.width + 300, height: 46)
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
    
    func pickerView(_ pickerView: UIPickerView, widthForComponent component: Int) -> CGFloat {
        return 40
    }
    
    func pickerView(_ pickerView: UIPickerView, rowHeightForComponent component: Int) -> CGFloat {
        return 160
    }
    
    func pickerView(_ in: UIPickerView, numberOfRowsInComponent: Int) -> Int {
        return _pickerViewArray.count
    }
    
    func pickerView(_ pickerView: UIPickerView, viewForRow row: Int, forComponent component: Int, reusing view: UIView?) -> UIView {
        let rect = CGRect(x: 0, y: 0, width: 160, height: 20)
        let label = UILabel(frame: rect)
        
        let rotate = CGAffineTransform(rotationAngle: CGFloat.pi / 2.0)
        //rotate.scaledBy(x: 0.25, y: 2.0)
        label.transform = rotate
        
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
    
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        guard !_pickerViewArray.isEmpty && row < _pickerViewArray.count else { return }
        listener?.onSelected(self, index: row, value: _pickerViewArray[row])
    }
    
    public var listener:CameraTypePickerDelegate?
    
    public var selectedItem:CameraTypeItem {
        get {
            let idx = selectedRow(inComponent: 0)
            let item = _pickerViewArray[idx]
            return item
        }
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

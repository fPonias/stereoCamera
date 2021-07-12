//
//  PopupButton.swift
//  stereoCamera
//
//  Created by Cody Munger on 5/8/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit

class PopupButton : UIButton {
    required init?(coder: NSCoder) {
        super.init(coder: coder)
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    private var _modalOriginOffset = CGPoint()
    var modalOrginOffset:CGPoint {
        get { return _modalOriginOffset }
        set(value) {
            _modalOriginOffset = value
            caluclateModalOffset()
        }
    }
    
    var isShowing = false
    var _modal:PopupButtonModal?
    var modal:PopupButtonModal? {
        get { return _modal }
    }
    
    var _rootView:UIView?
    var rootView:UIView? {
        get { return _rootView }
        set(value) {
            _rootView = value
            caluclateModalOffset()
        }
    }
    
    var _delegate:PopupButtonDelegate?
    var delegate:PopupButtonDelegate? {
        get { return _delegate }
        set(value) { _delegate = value }
    }
    
    private func caluclateModalOffset()
    {
        guard let rootView = _rootView,
              let modal = _modal
        else { return }
        
        var offset = _modalOriginOffset
        var view = self as UIView?
        while view != nil && view != rootView {
            offset = CGPoint(x: offset.x + view!.frame.origin.x, y: offset.y + view!.frame.origin.y)
            view = view!.superview
        }
        
        modal.origin = offset
    }
    
    override func didMoveToSuperview() {
        addTarget(self, action: #selector(onTapped), for: .primaryActionTriggered)
    }
    
    @objc func onTapped() {
        if isShowing {
            close()
        } else {
            open()
        }
    }
    
    public func open() {
        guard let modal = modal,
              let rootView = _rootView,
              !isShowing
        else { return }
    
        rootView.addSubview(modal)
        isShowing = true
    }
    
    public func close() {
        guard let modal = modal,
              let rootView = _rootView,
              isShowing
        else { return }
        
        rootView.willRemoveSubview(modal)
        modal.removeFromSuperview()
        isShowing = false
    }
    
    public var selectedItem:Any? {
        get {
            guard let modal = _modal else { return nil }
            return modal.selectedItem
        }
    }
    
    public func createModal() -> PopupButtonModal {
        if let modal = _modal {
            return modal
        }
        
        _modal = PopupButtonModal(parent: self)
        caluclateModalOffset()
        return modal!
    }
}

class PopupButtonDelegate {
    let target:PopupButtonPicker
    
    init (target:PopupButtonPicker) {
        self.target = target
    }
        
    func itemSelected(_ button:PopupButton, value:Any?) {}
    
    func getTitle(selected:PopupButtonModalPickerItem) -> String {
        return "< \(selected.text)"
    }
}

protocol PopupButtonModalPickerItem {
    var text:String { get }
}

class PopupButtonModal : UIView {
    private var _origin = CGPoint()
    var origin:CGPoint {
        get { return _origin }
        set(value) {
            _origin = value
            frame.origin = _origin
            setNeedsLayout()
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
    }
    
    init(parent: PopupButton) {
        self.parent = parent
        super.init(frame: CGRect())
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public var parent:PopupButton?
    
    private var _selectedItem:Any = 0
    public var selectedItem:Any {
        get { return _selectedItem }
        set(value) {
            _selectedItem = value
            setNeedsDisplay()
        }
    }
}



//
//  ClickableButton.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/7/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class ClickableButton : UIButton
{
    init()
    {
        super.init(frame: .zero)
        doInit()
    }

    override init(frame: CGRect)
    {
        super.init(frame:frame)
        doInit()
    }
    
    required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
        doInit()
    }
    
    private func doInit()
    {
        addTarget(self, action: #selector(ClickableButton.clickAction), for: .touchUpInside)
    }
    
    @objc func clickAction()
    {
        if (listener == nil)
            { return }
        
        listener!(self)
    }
    
    private var listener:Optional<(ClickableButton)->Void>
    func onClick(_ action:@escaping (ClickableButton)->Void)
    {
        listener = action
    }
}

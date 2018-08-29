//
//  LoadingPopupCtrl.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/6/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class LoadingPopupCtrl : UIViewController
{
    @IBOutlet weak var backgroundOverlay: UIView!
    @IBOutlet weak var loadSpinner: LoadSpinner!
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var buttonList: UIStackView!
    
    static func initFromStoryboard() -> LoadingPopupCtrl
    {
        let sb = UIStoryboard.init(name: "Main", bundle: nil)
        let ret = sb.instantiateViewController(withIdentifier: "LoadingPopup")
        
        ret.providesPresentationContextTransitionStyle = true
        ret.definesPresentationContext = true
        ret.modalPresentationStyle = UIModalPresentationStyle.overCurrentContext
    
        return ret as! LoadingPopupCtrl
    }
    
    override func viewDidLoad()
    {
        titleLabel.text = _header
        
        for (button, _) in actions
        {
            buttonList?.addArrangedSubview(button)
        }
        
        buttonList?.translatesAutoresizingMaskIntoConstraints = false
    }
    
    override func viewWillDisappear(_ animated: Bool)
    {
        loadSpinner.stop()
        actions.removeAll()
        buttons.removeAll()
    }
    
    override func viewWillAppear(_ animated: Bool)
    {
        loadSpinner.start()
    }
    
    private var _header:String = ""
    var header: String
    {
        get { return _header }
        set
        {
            _header = newValue
            
            DispatchQueue.main.async
            { [unowned self] in
                self.titleLabel?.text = self._header
            }
        }
    }
    
    class Action
    {
        static var nextId:Int = 1
        var id:Int
        var text:String
        var onClick:(LoadingPopupCtrl, Action)->Void
    
        init(text:String, onClick:@escaping (LoadingPopupCtrl, Action)->Void)
        {
            id = Action.nextId
            Action.nextId += 1
            
            self.text = text
            self.onClick = onClick
        }
    }
 
    private var actions = [ClickableButton:Action]()
    private var buttons = [ClickableButton]()
 
    func addAction(action:Action)
    {
        let btn = ClickableButton()
        btn.setTitle(action.text, for: .normal)
        btn.setTitleColor(UIColor.blue, for: .normal)
        btn.onClick(buttonPressed)
        //btn.widthAnchor.constraint(equalToConstant: 100).isActive = true
        //btn.heightAnchor.constraint(equalToConstant: 30).isActive = true
        
        buttonList?.addArrangedSubview(btn)
        buttonList?.translatesAutoresizingMaskIntoConstraints = false
        
        actions[btn] = action
        buttons.append(btn)
    }
    
    func buttonPressed(_ button:ClickableButton)
    {
        let action = actions[button]
        
        if (action == nil)
            { return }
        
        action?.onClick(self, action!)
    }
}

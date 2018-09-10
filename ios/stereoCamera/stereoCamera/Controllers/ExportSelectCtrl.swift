//
//  ExportSelectCtrl.swift
//  stereoCamera
//
//  Created by hallmarklabs on 9/7/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class ExportSelectCtrl : UIViewController, UICollectionViewDelegate, UICollectionViewDataSource
{
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int
    {
        if (section == 0)
            { return selections.count}
        else
            { return 0 }
    }
    
    func numberOfSections(in collectionView: UICollectionView) -> Int
    {
        return 1
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell
    {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "radioSelectionCell", for: indexPath)
        guard (cell is RadioSelection) else { return cell }
        
        let cell2 = cell as! RadioSelection
        cell2.parent = self
        let selection = selections[indexPath.item]
        
        cell2.selection = selection
        if (currentSelection != nil && currentSelection!.id == selection.id)
            { cell2.isHilighted = true }
        else
            { cell2.isHilighted = false }
        
        cell2.widthAnchor.constraint(equalToConstant: collectionView.bounds.size.width).isActive = true
        cell2.heightAnchor.constraint(equalToConstant: 30).isActive = true
        
        return cell2
    }
    
    func onSelectionTapped(cell:RadioSelection)
    {
        currentSelection = cell.selection
    }
    
    @IBOutlet weak var buttonList: UIStackView!
    @IBOutlet weak var selectionList: UICollectionView!
    @IBOutlet weak var titleLabel: UILabel!
    
    static func initFromStoryboard() -> ExportSelectCtrl
    {
        let sb = UIStoryboard.init(name: "Main", bundle: nil)
        let ret = sb.instantiateViewController(withIdentifier: "ExportSelectPopup")
        
        ret.providesPresentationContextTransitionStyle = true
        ret.definesPresentationContext = true
        ret.modalPresentationStyle = UIModalPresentationStyle.overCurrentContext
    
        return ret as! ExportSelectCtrl
    }
    
    override func viewDidLoad()
    {
        titleLabel.text = _header
        
        for (button, _) in actions
        {
            buttonList?.addArrangedSubview(button)
        }
        
        buttonList?.translatesAutoresizingMaskIntoConstraints = false
        
        selectionList.delegate = self
        selectionList.dataSource = self
    }
    
    override func viewWillDisappear(_ animated: Bool)
    {
        actions.removeAll()
        buttons.removeAll()
        selections.removeAll()
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
        var onClick:(ExportSelectCtrl, Action)->Void
    
        init(text:String, onClick:@escaping (ExportSelectCtrl, Action)->Void)
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
    
    private var selections = [Selection]()
    
    class Selection
    {
        static var nextId:Int = 1
        var id:Int
        var text:String
    
        init(text:String)
        {
            id = Selection.nextId
            Selection.nextId += 1
            
            self.text = text
        }
    }
    
    @IBOutlet weak var listHeightConstraint: NSLayoutConstraint!
    
    func setSelections(_ selections:[Selection])
    {
        self.selections = selections
        
        if (_currentSelection == nil && self.selections.count > 0)
            { _currentSelection = self.selections[0] }
        
        DispatchQueue.main.async {
        [unowned self] in
            let height = CGFloat(34 * self.selections.count)
            self.listHeightConstraint.constant = height
            self.selectionList.reloadData()
            self.selectionList.collectionViewLayout.invalidateLayout()
        }
    }
    
    private var _currentSelection:Selection? = nil
    var currentSelection:Selection?
    {
        get { return _currentSelection }
        set
        {
            _currentSelection = newValue
            
            DispatchQueue.main.async {
            [unowned self] in
                self.selectionList.reloadData()
            }
        }
    }
}

class RadioSelection : UICollectionViewCell
{
    @IBOutlet weak var icon: UIImageView!
    @IBOutlet weak var label: UILabel!
    
    public override init(frame: CGRect)
    {
        super.init(frame: frame)
        doInit()
    }

    public required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
        doInit()
    }
    
    func doInit()
    {
        let inter = UITapGestureRecognizer(target: self, action: #selector(RadioSelection.cellTapped))
        addGestureRecognizer(inter)
    }
    
    private var _selection:ExportSelectCtrl.Selection?
    var selection:ExportSelectCtrl.Selection?
    {
        get { return _selection }
        set
        {
            _selection = newValue
            
            if (_selection != nil)
            {
                label.text = _selection!.text
            }
            else
            {
                label.text = ""
            }
        }
    }
    
    var parent:ExportSelectCtrl? = nil
    
    override func preferredLayoutAttributesFitting(_ layoutAttributes: UICollectionViewLayoutAttributes) -> UICollectionViewLayoutAttributes
    {
        if (parent == nil)
            { return layoutAttributes }
    
        var frame = layoutAttributes.frame
        frame.size.width = parent!.selectionList.frame.size.width
        frame.size.height = 30
        
        layoutAttributes.frame = frame
        return layoutAttributes
    }
    
    private var _isHilighted:Bool = false
    var isHilighted:Bool
    {
        get { return _isHilighted }
        set
        {
            _isHilighted = newValue
            
            let img:UIImage
            if (_isHilighted)
                { img = UIImage(named: "radio_on")! }
            else
                { img = UIImage(named: "radio_off")! }
            
            icon.image = img
        }
    }
    
    @objc func cellTapped()
    {
        parent?.onSelectionTapped(cell: self)
    }
}

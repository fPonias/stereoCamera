//
//  SettingsListSelectorCtrl.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/29/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class SettingsListSelectorCtrl : UIViewController, UICollectionViewDataSource, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout
{
    @IBOutlet weak var optionsList: UICollectionView!
    @IBOutlet weak var saveBtn: UIBarButtonItem!
    
    class dataItem : Hashable {
        static func == (lhs: SettingsListSelectorCtrl.dataItem, rhs: SettingsListSelectorCtrl.dataItem) -> Bool {
            return lhs.title == rhs.title
        }
        
        func hash(into hasher: inout Hasher) {
            hasher.combine(title)
        }
        
        let title:String
        let value:Any
        var selected:Bool
        
        init(title:String, value:Any, selected:Bool) {
            self.title = title
            self.value = value
            self.selected = selected
        }
    }
    
    var data = [dataItem]()
    
    private var _multiSelect = false
    var multiSelect:Bool {
        get { return _multiSelect }
        set {
            _multiSelect = newValue
            if !multiSelect {
                navigationItem.rightBarButtonItem = nil
            }
        }
    }
 
    private func fillData()
    {
        
    }
 
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        optionsList.dataSource = self
        optionsList.delegate = self
        multiSelect = multiSelect
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int
    {
        return data.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell
    {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "optionButton", for: indexPath)
        let btnCell = cell as! SettingsListWidget
        let idx = indexPath.item
        let value = data[idx]
        btnCell.setData(id: idx, title: value.title, selected: value.selected)
        btnCell.setTapListener(cellTapped)

        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize
    {
        let sz = CGSize(width: collectionView.bounds.size.width, height: 30.0)
        return sz
    }
    
    @IBAction func saveTapped(_ sender: Any) {
        var ret = Set<dataItem>()
        
        for datum in data {
            if (datum.selected) {
                ret.insert(datum)
            }
        }
        
        savedHandler?(self, ret)
        
        DispatchQueue.main.async {
        [unowned self] in
            self.navigationController?.popViewController(animated: true)
        }
    }
    
    func cellTapped(idx:Int)
    {
        data[idx].selected = !data[idx].selected
        optionsList.reloadItems(at: [IndexPath(item: idx, section: 0)])
        
        cellTappedHandler?(self, data[idx])
        
        if (!multiSelect) {
            DispatchQueue.main.async {
            [unowned self] in
                self.navigationController?.popViewController(animated: true)
            }
        }
    }
    
    var cellTappedHandler:((SettingsListSelectorCtrl, dataItem) -> Void)? = nil
    var savedHandler:((SettingsListSelectorCtrl, Set<dataItem>) -> Void)? = nil
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath)
    {
    
        DispatchQueue.main.async {
        [unowned self] in
            self.navigationController?.popViewController(animated: true)
        }
    }
}

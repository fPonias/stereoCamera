//
//  SettingsListCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 1/9/23.
//  Copyright © 2023 cody. All rights reserved.
//

import Foundation
import UIKit

class SettingsListCtrl : NSObject, UICollectionViewDataSource, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout {
    class dataItem : Hashable {
        static func == (lhs: dataItem, rhs: dataItem) -> Bool {
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
    
    var target: UICollectionView
    var data = [dataItem]()
    
    init(target: UICollectionView) {
        self.target = target
    }
    
    private var _multiSelect = false
    var multiSelect:Bool {
        get { return _multiSelect }
        set {
            _multiSelect = newValue
        }
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
    
    func cellTapped(idx:Int)
    {
        data[idx].selected = !data[idx].selected
        target.reloadItems(at: [IndexPath(item: idx, section: 0)])
        
        cellTappedHandler?(self, data[idx])
    }
    
    var cellTappedHandler:((SettingsListCtrl, dataItem) -> Void)? = nil
}

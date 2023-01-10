//
//  FilterSelectListCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 1/9/23.
//  Copyright © 2023 cody. All rights reserved.
//

import UIKit
import Foundation

class FilterSelectListCtrl : UICollectionView, UICollectionViewDataSource, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout {
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        loadNibs()
    }
    
    override init(frame: CGRect, collectionViewLayout layout: UICollectionViewLayout) {
        super.init(frame: frame, collectionViewLayout: layout)
        loadNibs()
    }
    
    func loadNibs() {
        let nib = UINib(nibName: "SelectorCollectionWidget", bundle: nil)
        register(nib, forCellWithReuseIdentifier: "filterOption")
        
        dataSource = self
        delegate = self
    }
    
    public class OptionItem : Hashable {
        static func == (lhs: FilterSelectListCtrl.OptionItem, rhs: FilterSelectListCtrl.OptionItem) -> Bool {
            return lhs.value == rhs.value
        }
        
        func hash(into hasher: inout Hasher) {
            hasher.combine(value)
        }
        
        
        let label: String
        let value: Int
        var selected: Bool
        var enabled: Bool
        
        init(label: String, value: Int, selected: Bool, enabled: Bool = true) {
            self.label = label
            self.value = value
            self.selected = selected
            self.enabled = enabled
        }
    }
    
    private var _options = [OptionItem]()
    var options: [OptionItem] {
        get { return _options }
        set {
            _options = newValue
        }
    }

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return options.count
    }
    
    var cellMap = [IndexPath: FilterSelectWidget]()
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "filterOption", for: indexPath)
        let btnCell = cell as! FilterSelectWidget
        btnCell.onTapped = cellTapped

        cellMap[indexPath] = btnCell
        updateCell(indexPath)
        
        return cell
    }
    
    private func updateCell(_ indexPath: IndexPath) {
        guard let btnCell = cellMap[indexPath] else { return }
        let idx = indexPath.item
        let value = options[idx]
        btnCell.setData(id: idx, title: value.label, selected: value.selected)
        btnCell.enabled = value.enabled
    }
    
    func collectionView(_ collectionView: UICollectionView, didEndDisplaying cell: UICollectionViewCell, forItemAt indexPath: IndexPath) {
        cellMap[indexPath] = nil
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize
    {
        let sz = CGSize(width: collectionView.bounds.size.width, height: 30.0)
        return sz
    }
    
    func cellTapped(widget: FilterSelectWidget)
    {
        let idx = widget.id
        let item = options[idx]
        item.selected = !item.selected
        
        reloadData()
    }
}

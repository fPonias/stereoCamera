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
    
    private var _type = Cookie.PrefType.OVERLAY
    
    var type : Cookie.PrefType
    {
        get { return _type }
        set
        {
            _type = newValue
            fillData()
            
            if (optionsList != nil)
                {optionsList.reloadData() }
        }
    }
    
    var data = [String]()
 
    private func fillData()
    {
        data = [String]()
        var i = 0
        while (true)
        {
            if (_type == .OVERLAY)
            {
                let enumType = Overlay(rawValue: i)
                
                if (enumType == nil)
                    { break }
                
                data.append(enumType!.toString())
            }
            else if (_type == .IMAGE_QUALITY)
            {
                let enumType = ImageQuality(rawValue: i)
                
                if (enumType == nil)
                    { break }
                
                data.append(enumType!.toString())
            }
            
            i += 1
        }
    }
 
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        optionsList.dataSource = self
        optionsList.delegate = self
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
        btnCell.setData(id: idx, title: value)
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
        if (_type == .OVERLAY)
        {
            let enumType = Overlay(rawValue: idx)
            Cookie.instance.overlay = enumType!
        }
        else if (_type == .IMAGE_QUALITY)
        {
            let imgQual = ImageQuality(rawValue: idx)
            Cookie.instance.imageQuality = imgQual!
        }
        
        if (cellTappedHandler != nil)
            { cellTappedHandler!(self, idx) }
        
        DispatchQueue.main.async {
        [unowned self] in
            self.navigationController?.popViewController(animated: true)
        }
    }
    
    private var cellTappedHandler:Optional<(SettingsListSelectorCtrl, Int) -> Void> = nil
    func setCellTappedHandler(_ listener:@escaping (SettingsListSelectorCtrl, Int) -> Void)
    {
        cellTappedHandler = listener
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath)
    {
        switch _type
        {
        case .OVERLAY:
            let value = Overlay(rawValue: indexPath.item)!
            Cookie.instance.overlay = value
        case .IMAGE_QUALITY:
            let value = ImageQuality(rawValue: indexPath.item)!
            Cookie.instance.imageQuality = value
        }
    
        DispatchQueue.main.async {
        [unowned self] in
            self.navigationController?.popViewController(animated: true)
        }
    }
}

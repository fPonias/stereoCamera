//
//  FilterSelectCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 1/3/23.
//  Copyright © 2023 cody. All rights reserved.
//

import Foundation
import UIKit
import Photos

class FilterSelectCtrl : UIViewController
{
    
    @IBOutlet weak var optionsList: FilterSelectListCtrl!
    @IBOutlet weak var optionsListHeight: NSLayoutConstraint!
    @IBOutlet weak var mediaTypesList: FilterSelectListCtrl!
    @IBOutlet weak var mediaTypesListHeight: NSLayoutConstraint!
    
    private var encodingTypeOptions = [FilterSelectListCtrl.OptionItem]()
    private var mediaTypeOptions = [FilterSelectListCtrl.OptionItem]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        updateOptions()
        updateMediaTypes()
    }
    
    private func updateOptions() {
        let current = Cookie.instance.galleryFilter ?? Set()
        
        encodingTypeOptions.removeAll()
       
        for item in ImageFormat.allCases {
            if (item == .UNKNOWN) { continue }
            if (!Files.instance.hasType(item)) { continue }
           
            let option = FilterSelectListCtrl.OptionItem(label: item.toString(), value: item.rawValue, selected: current.contains(item))
            encodingTypeOptions.append(option)
        }
        
        optionsList.options = encodingTypeOptions
        optionsListHeight.constant = CGFloat(40 * encodingTypeOptions.count)
    }
    
    private func updateMediaTypes() {
        let current = Cookie.instance.mediaTypesFilter ?? Set()
        let labels = Dictionary(dictionaryLiteral: (PHAssetMediaType.video, "Video"), (PHAssetMediaType.image, "Photo"))
        
        mediaTypeOptions.removeAll()
        
        for item in Cookie.availableMediaTypes {
            let option = FilterSelectListCtrl.OptionItem(label: labels[item] ?? "Unknown", value: item.rawValue, selected: current.contains(item))
            mediaTypeOptions.append(option)
        }
        
        mediaTypesList.options = mediaTypeOptions
        mediaTypesListHeight.constant = CGFloat(40 * mediaTypeOptions.count)
    }
    
    @IBAction func filterClicked(_ sender: Any) {
        let sum:Set<ImageFormat> = optionsList.options.reduce(into: Set<ImageFormat>(), { sum, option in
            guard let item = ImageFormat(rawValue: option.value),
                  option.selected
            else { return }
            
            sum.insert(item)
        })
        
        Cookie.instance.galleryFilter = sum
        
        let types = mediaTypesList.options.reduce(into: Set<PHAssetMediaType>(), { types, option in
            guard let item = PHAssetMediaType(rawValue: option.value),
                  option.selected
            else { return }
            
            types.insert(item)
        })
        
        Cookie.instance.mediaTypesFilter = types
        
        dismiss(animated: true)
        onDismissed?()
    }
    
    var onDismissed:(() -> Void)? = nil
}

//
//  SettingsCtrl.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/25/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class SettingsCtrl : UIViewController
{
    @IBOutlet weak var photoFormatBtn: UIButton!
    @IBOutlet weak var photoQualityBtn: UIButton!
    //@IBOutlet weak var videoFormatBtn: UIButton!
    @IBOutlet weak var videoQualityBtn: UIButton!
    @IBOutlet weak var preferredOrientationBtn: UIButton!
    
    override func viewDidLoad()
    {
        updateButtons()
    }
    
    func updateButtons()
    {
        let photoFormat = Cookie.instance.photoFormat
        if (photoFormat.count == 1) {
            let title = photoFormat.first!.toString()
            photoFormatBtn.setTitle(title, for: .normal)
        } else if !photoFormat.isEmpty {
            photoFormatBtn.setTitle("multiple", for: .normal)
        } else {
            photoFormatBtn.setTitle("none", for: .normal)
        }
        
        let photoQuality = Cookie.instance.photoImageQuality
        photoQualityBtn.setTitle(photoQuality.toString(), for: .normal)
        
        //let videoFormat = Cookie.instance.videoFormat
        //videoFormatBtn.setTitle(videoFormat.toString(), for: .normal)
        
        let videoQuality = Cookie.instance.videoImageQuality
        videoQualityBtn.setTitle(videoQuality.toString(), for: .normal)
        
        let orientation = Cookie.instance.preferredOrientation
        let orientTitle = orientation == .portrait ? "vertical" : "horizontal"
        preferredOrientationBtn.setTitle(orientTitle, for: .normal)
    }
    
    private func openPopup(type:Cookie.PrefType)
    {
        performSegue(withIdentifier: "SettingsListSelectorSegue", sender: type)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        //why the hell does this code go in the source controller?
        if (segue.identifier == "SettingsListSelectorSegue")
        {
            guard let ctrl = segue.destination as? SettingsListSelectorCtrl,
                  let type = sender as? Cookie.PrefType
            else { return }
            
            switch(type) {
            case .ORIENTATION:
                let sel = Cookie.instance.preferredOrientation
                ctrl.data = [
                SettingsListSelectorCtrl.dataItem(
                    title: "horizontal",
                    value: UIDeviceOrientation.landscapeRight,
                    selected: sel == UIDeviceOrientation.landscapeRight
                ),
                SettingsListSelectorCtrl.dataItem(
                    title: "vertival",
                    value: UIDeviceOrientation.portrait,
                    selected: sel == UIDeviceOrientation.portrait
                )
            ]
            case .PHOTO_IMAGE_QUALITY:
                let sel = Cookie.instance.photoImageQuality
                ctrl.data.removeAll()
                for qual in ImageQuality.allCases {
                    ctrl.data.append(SettingsListSelectorCtrl.dataItem(
                        title: qual.toString(),
                        value: qual,
                        selected: sel == qual
                    ))
                }
            case .VIDEO_IMAGE_QUALITY:
                let sel = Cookie.instance.videoImageQuality
                ctrl.data.removeAll()
                for qual in ImageQuality.allCases {
                    ctrl.data.append(SettingsListSelectorCtrl.dataItem(
                        title: qual.toString(),
                        value: qual,
                        selected: sel == qual
                    ))
                }
            case .PHOTO_IMAGE_FORMAT:
                let sel = Cookie.instance.photoFormat
                ctrl.data.removeAll()
                for fmt in ImageFormat.allCases {
                    ctrl.data.append(SettingsListSelectorCtrl.dataItem(
                        title: fmt.toString(),
                        value: fmt,
                        selected: sel.contains(fmt)
                    ))
                }
                ctrl.multiSelect = true
            case .VIDEO_IMAGE_FORMAT:
                let sel = Cookie.instance.videoFormat
                ctrl.data.removeAll()
                for fmt in ImageFormat.allCases {
                    ctrl.data.append(SettingsListSelectorCtrl.dataItem(
                        title: fmt.toString(),
                        value: fmt,
                        selected: sel == fmt
                    ))
                }
            }
            
            ctrl.cellTappedHandler = {[weak self] ctrl, data in
                if type == .ORIENTATION {
                    if let ori = data.value as? UIDeviceOrientation {
                        Cookie.instance.preferredOrientation = ori
                    }
                } else if type == .PHOTO_IMAGE_QUALITY {
                    if let qual = data.value as? ImageQuality {
                        Cookie.instance.photoImageQuality = qual
                    }
                } else if type == .VIDEO_IMAGE_QUALITY {
                    if let qual = data.value as? ImageQuality {
                        Cookie.instance.videoImageQuality = qual
                    }
                } else if type == .VIDEO_IMAGE_FORMAT {
                    if let qual = data.value as? ImageFormat {
                        Cookie.instance.videoFormat = qual
                    }
                }
                
                self?.updateButtons()
            }
            
            ctrl.savedHandler = {[weak self] ctrl, data in
                if type == .PHOTO_IMAGE_FORMAT {
                    var fmtSet = Set<ImageFormat>()
                    
                    for datum in data {
                        if let fmt = datum.value as? ImageFormat {
                            fmtSet.insert(fmt)
                        }
                    }
                    
                    Cookie.instance.photoFormat = fmtSet
                }
                
                self?.updateButtons()
            }
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }
    
    override func viewWillDisappear(_ animated: Bool)
    {
        super.viewWillDisappear(animated)
    
        let idx = navigationController?.viewControllers.index(of: self)
        if (idx == nil)
        {
            if (doneListener != nil)
                { doneListener!() }
        }
        
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    private var doneListener:Optional<() -> Void> = nil
    func setSettingsDoneHandler(_ listener:@escaping () -> Void)
    {
        doneListener = listener
    }
    
    func cellSelected(target:SettingsListSelectorCtrl, idx: Int)
    {
        updateButtons()
    }
    
    @IBAction func photoFormatTapped(_ sender: Any) {
        openPopup(type: .PHOTO_IMAGE_FORMAT)
    }
    
    @IBAction func photoQualityTapped(_ sender: Any) {
        openPopup(type: .PHOTO_IMAGE_QUALITY)
    }
    
    @IBAction func videoFormatTapped(_ sender: Any) {
        openPopup(type: .VIDEO_IMAGE_FORMAT)
    }
    
    @IBAction func videoQualityTapped(_ sender: Any) {
        openPopup(type: .VIDEO_IMAGE_QUALITY)
    }
    
    @IBAction func preferredOrientationTapped(_ sender: Any) {
        openPopup(type: .ORIENTATION)
    }
}

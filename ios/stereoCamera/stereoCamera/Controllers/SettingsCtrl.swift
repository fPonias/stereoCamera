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
    @IBOutlet weak var previewOverlayBtn: UIButton!
    @IBOutlet weak var imageQualityBtn: UIButton!
    @IBOutlet weak var syncTestSwitch: UISwitch!
    
    override func viewDidLoad()
    {
        updateButtons()
    }
    
    func updateButtons()
    {
        let imgQual = Cookie.instance.imageQuality
        imageQualityBtn.setTitle(imgQual.toString(), for: UIControlState.normal)
        
        let syncTest = Cookie.instance.runSyncTest
        syncTestSwitch.setOn(syncTest, animated: false)
        
        let overlay = Cookie.instance.overlay
        previewOverlayBtn.setTitle(overlay.toString(), for: UIControlState.normal)
    }
    
    @IBAction func previewOverlayAct(_ sender: Any)
    {
        openPopup(type: .OVERLAY)
    }
    
    @IBAction func imageQualityAct(_ sender: Any)
    {
        openPopup(type: .IMAGE_QUALITY)
    }
    
    @IBAction func syncTestAct(_ sender: Any)
    {
        Cookie.instance.runSyncTest = syncTestSwitch.isOn
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
            guard let destCtrl = segue.destination as? SettingsListSelectorCtrl else { return }
            guard let obj = sender as? Cookie.PrefType else { return }
            destCtrl.type = obj
            
            destCtrl.setCellTappedHandler(cellSelected)
        }
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
}

//
//  MediaExporter.swift
//  stereoCamera
//
//  Created by Cody Munger on 12/12/22.
//  Copyright © 2022 cody. All rights reserved.
//

import Foundation
import UIKit
import Photos

class MediaExporter {
    private let SELECTION_COPY = "Copy"
    private let SELECTION_INSTA = "Instagram horizontal"
    private let SELECTION_ROTATE_INSTA = "Instagram rotated"
    
    private var files:[PHAsset]
    private let ctrl:UIViewController
    private var showSelector = false
    private let queue = DispatchQueue(label: "Media Exporter thread")
    
    init(ctrl: UIViewController) {
        self.ctrl = ctrl
        files = [PHAsset]()
        toShare = [Any?]()
        exportCount = 0
    }
    
    func exportAction(files: [PHAsset])
    {
        self.files = files
        toShare = [Any?](repeating: nil, count: files.count)
        exportCount = 0
        
        guard !files.isEmpty else { return }
        
        for file in files {
            if file.mediaType == .image && file.playbackStyle != .imageAnimated {
                showSelector = true
                break
            }
        }
        
        if !showSelector {
            exportClicked2(.COPY)
            return
        }
        
        
        let popup = ExportSelectCtrl.initFromStoryboard()
        
        popup.header = "Export Actions"

        var selections = [ExportSelectCtrl.Selection]()
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_COPY))
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_INSTA))
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_ROTATE_INSTA))
        popup.setSelections(selections)
        
        popup.addAction(action: ExportSelectCtrl.Action(text: "Cancel", onClick: {
        [unowned self] (popup, action) in
            self.ctrl.dismiss(animated: true, completion: nil)
        }))
        
        popup.addAction(action: ExportSelectCtrl.Action(text: "Export", onClick: {
        [unowned self] (popup, action) in
            
            var type = ImageProvider.ExportType.COPY
            let selection = popup.currentSelection
            
            if (selection != nil)
            {
                if (selection!.text == self.SELECTION_ROTATE_INSTA)
                    { type = ImageProvider.ExportType.ROTATE_TO_PORTRAIT }
                else if (selection!.text == self.SELECTION_INSTA) {
                    type = ImageProvider.ExportType.SCALE_FOR_INSTAGRAM }
                else
                    { type = ImageProvider.ExportType.COPY }
            }
            
            self.ctrl.dismiss(animated: true, completion: nil)
            self.exportClicked2(type)
        }))
        
        ctrl.present(popup, animated: true, completion: nil)
    }

    private var toShare:[Any?]
    
    private func exportClicked2(_ type:ImageProvider.ExportType) {
        queue.sync {
            toShare = [Any?](repeating: nil, count: files.count)
            exportCount = 0
            
            queue.async {
                for i in 0 ..< self.files.count {
                    self.handleFile(i, type)
                }
            }
        }
    }
    
    private func handleFile(_ index:Int, _ type:ImageProvider.ExportType)
    {
        let file = files[index]
        if (file.mediaType == .video) || (file.mediaType == .image && file.playbackStyle == .imageAnimated) {
            Files.instance.assetToUrl(file, completed: {[weak self] video in
                guard let self = self,
                      let video = video
                else { return }
                
                self.exportClicked3(index, video)
            })
        } else {
            Files.instance.assetToImage(file, completed: {[weak self] img in
                guard let self = self,
                      let image = img
                else { return }
                
                let asset = ImageProvider(placeholderItem: image, type: type)
                self.exportClicked3(index, asset)
            })
        }
    }
    
    private var exportCount = 0
    
    private func exportClicked3(_ index:Int, _ asset: Any) {
        queue.sync {
            exportCount += 1
            toShare[index] = asset
            
            guard(exportCount == files.count) else { return }
            
            for asset in toShare {
                guard asset != nil else { return }
            }
            
            queue.async {
                let shareCtrl = UIActivityViewController(activityItems: self.toShare as [Any], applicationActivities: nil)
                
                DispatchQueue.main.async {
                    self.ctrl.present(shareCtrl, animated: true, completion: nil)
                }
            }
        }
    }
}

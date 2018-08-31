//
//  SettingsHtmlCtrl.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/30/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import AVKit
import WebKit

class SettingsHtmlCtrl : UIViewController
{
    @IBOutlet weak var htmlView: WKWebView!
    
    override func viewDidLoad()
    {
        let url = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "assets")
        
        htmlView.loadFileURL(url!, allowingReadAccessTo: url!.deletingLastPathComponent())
    }
}

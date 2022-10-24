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
    private var navDelegate = WebDelegate()
    
    override func viewDidLoad()
    {
        let url = Bundle.main.url(forResource: "about", withExtension: "html", subdirectory: "assets")
        
        htmlView.loadFileURL(url!, allowingReadAccessTo: url!.deletingLastPathComponent())
        htmlView.navigationDelegate = navDelegate
    }
    
    class WebDelegate : NSObject, WKNavigationDelegate
    {
        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void)
        {
            guard navigationAction.navigationType == .linkActivated,
                  let url = navigationAction.request.url,
                  let host = url.host,
                  UIApplication.shared.canOpenURL(url)
            else {
                decisionHandler(.allow)
                return
            }
            
            UIApplication.shared.open(url)
            print(url)
            print("Redirected to browser. No need to open it locally")
            decisionHandler(.cancel)
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }
}

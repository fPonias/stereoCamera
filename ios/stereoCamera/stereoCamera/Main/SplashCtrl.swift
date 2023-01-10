//
//  SplashCtrl.swift
//  stereoCamera
//
//  Created by Cody Munger on 1/2/23.
//  Copyright © 2023 cody. All rights reserved.
//

import Foundation
import UIKit

class SplashCtrl : UIViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        
        Files.instance.indexImages(completed: { [weak self] in
            guard let self = self else { return }
            self.performSegue(withIdentifier: "mainSegue", sender: self)
        })
    }
}

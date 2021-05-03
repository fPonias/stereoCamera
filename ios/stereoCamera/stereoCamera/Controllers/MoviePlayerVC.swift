//
//  MoviePlayerVC.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/27/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import AVKit

class MoviePlayerVC : AVPlayerViewController
{
    override func viewDidLoad() {
    }
    
    override func viewDidLayoutSubviews() {
        let fileManager = FileManager.default
        let urls = fileManager.urls(for: .documentDirectory, in: .userDomainMask)
        
        guard urls.count > 0 else { return }
        
        do {
            let fileURLs = try fileManager.contentsOfDirectory(at: urls[0], includingPropertiesForKeys: nil)
            
            guard fileURLs.count > 0 else { return }
            
            let attr = try fileManager.attributesOfItem(atPath: fileURLs[0].path)
            playVideo(url: fileURLs[0])
        } catch {
            print("Error while enumerating files \(urls[0].path): \(error.localizedDescription)")
        }
    }
    
    func playVideo(url: URL){
        let item = AVPlayerItem(url: url)
        player?.replaceCurrentItem(with: item)
        player?.play()
    }
}

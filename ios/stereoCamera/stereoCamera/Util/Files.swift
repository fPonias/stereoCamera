//
//  Files.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/20/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

public class Files
{
    public static func getTmpDir() -> URL
    {
        return FileManager.default.temporaryDirectory
    }
    
    public static func getDataDir() -> URL?
    {
        let outDirs:[URL] = FileManager.default.urls(for: FileManager.SearchPathDirectory.cachesDirectory, in: FileManager.SearchPathDomainMask.userDomainMask)
        guard (outDirs.count > 0) else { return nil }
        
        return outDirs[0]
    }
    
    public static func getRandomFile() -> URL?
    {
        let outDir = getDataDir()
        guard(outDir != nil) else { return nil }
        
        let id = arc4random()
        let path = outDir!.path + "/" + String(id)
        
        return URL(fileURLWithPath: path);
    }
}

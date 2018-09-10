//
//  Files.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/20/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import Photos

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
    
    
    static func getGalleryFiles() -> [PHAsset]
    {
        let galleryTitle:String = Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as! String
        var files = [PHAsset]()
        
        let fetchOptions = PHFetchOptions()
        fetchOptions.predicate = NSPredicate(format: "title = %@", galleryTitle)
        let collections = PHAssetCollection.fetchAssetCollections(with: PHAssetCollectionType.album, subtype: PHAssetCollectionSubtype.any, options: fetchOptions)
        guard (collections.count > 0) else { return files }
        
        let filesCollection = collections.firstObject!
        
        let collectionResult = PHAsset.fetchAssets(in: filesCollection, options: nil)
        let sz = collectionResult.count
        
        for i in 0 ... sz - 1
        {
            files.append(collectionResult.object(at: i))
        }
        
        return files
    }
    
    static func dateToLabel(_ date:Date) -> String
    {
        let fmt = DateFormatter()
        
        let cal = Calendar(identifier: .gregorian)
        let year = cal.component(.year, from: date)
        let now = Date(timeIntervalSinceNow: 0)
        let thisYear = cal.component(.year, from: now)
        
        let ret:String
        if (thisYear != year)
        {
            fmt.dateFormat = "MMM d yyyy"
            ret = fmt.string(from: date)
        }
        else
        {
            fmt.dateFormat = "MMM d"
            ret = fmt.string(from: date)
        }
        
        return ret
    }
    
    static func getGroupedGalleryFiles() -> ([String], [[PHAsset]])
    {
        var headers = [String]()
        var files = [[PHAsset]]()
    
        let data = getGalleryFiles()
        var currentLabel = ""
        var currentList = [PHAsset]()
        
        for asset in data
        {
            let label = dateToLabel(asset.creationDate!)
            
            if (label != currentLabel)
            {
                if (currentLabel != "")
                {
                    headers.insert(currentLabel, at: 0)
                    files.insert(currentList, at: 0)
                }
            
                currentLabel = label
                currentList = [PHAsset]()
            }
            
            currentList.append(asset)
        }
        
        if (currentLabel != "")
        {
            headers.insert(currentLabel, at: 0)
            files.insert(currentList, at: 0)
        }
        
        return (headers, files)
    }
    
    static func getSortedGalleryFiles() -> [PHAsset]
    {
        let (_, files) = getGroupedGalleryFiles()
        var ret = [PHAsset]()
    
        for group in files
        {
            ret.append(contentsOf: group)
        }
        
        return ret
    }
    
    static func assetToImage(_ asset: PHAsset, asThumbnail:Bool = false) -> UIImage
    {
        let manager = PHImageManager.default()
        let option = PHImageRequestOptions()
        var thumbnail = UIImage()
        option.isSynchronous = true
        
        let dims:CGSize
            
        if (!asThumbnail)
            { dims = CGSize(width: asset.pixelWidth, height: asset.pixelHeight)}
        else
        {
            let ratio = asset.pixelWidth / asset.pixelHeight
            let width = 120
            let height = width / ratio
            
            dims = CGSize(width: width, height: height)
        }
        
        manager.requestImage(for: asset, targetSize: dims, contentMode: PHImageContentMode.aspectFill, options: option, resultHandler: {(image: UIImage?, info:[AnyHashable : Any]?) -> Void
            in
            thumbnail = image!
        })
        
        return thumbnail
    }
    
    static func deleteAssets(_ assets:[PHAsset]) -> Bool
    {
        do
        {
            try PHPhotoLibrary.shared().performChangesAndWait(
            {
                let arr = NSArray(array: assets)
                PHAssetChangeRequest.deleteAssets(arr)
            })
        } catch {
            print("Failed to delete selected files")
            return false
        }
        
        return true
    }
}

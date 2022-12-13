//
//  Files.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/20/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import Photos
import UIKit

public protocol FilesDelegate {
    func onNewFile(asset: PHAsset)
}

public class Files
{
    static let instance:Files = Files()
    
    private init() {}
    
    private var imgManager = PHCachingImageManager()
    private var delegates = MulticastDelegate<FilesDelegate>()
    public func addDelegate(_ delegate:FilesDelegate) {
        delegates.add(delegate)
    }
    
    public func getTmpDir() -> URL
    {
        return FileManager.default.temporaryDirectory
    }
    
    public func getDataDir() -> URL?
    {
        let outDirs:[URL] = FileManager.default.urls(for: FileManager.SearchPathDirectory.cachesDirectory, in: FileManager.SearchPathDomainMask.userDomainMask)
        guard (outDirs.count > 0) else { return nil }
        
        return outDirs[0]
    }
    
    public func getRandomFile() -> URL?
    {
        let outDir = getDataDir()
        guard(outDir != nil) else { return nil }
        
        let id = arc4random()
        let path = outDir!.path + "/" + String(id)
        
        return URL(fileURLWithPath: path);
    }
    
    
    func getGalleryFiles() -> [PHAsset]
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
        guard sz > 0 else { return files }
        
        
        for i in 0 ... sz - 1
        {
            files.append(collectionResult.object(at: i))
        }
        
        return files
    }
    
    func dateToLabel(_ date:Date) -> String
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
    
    func getGroupedGalleryFiles() -> ([String], [[PHAsset]])
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
            
            currentList.insert(asset, at: 0)
        }
        
        if (currentLabel != "")
        {
            headers.insert(currentLabel, at: 0)
            files.insert(currentList, at: 0)
        }
        
        return (headers, files)
    }
    
    func getSortedGalleryFiles() -> [PHAsset]
    {
        let (_, files) = getGroupedGalleryFiles()
        var ret = [PHAsset]()
    
        for group in files
        {
            ret.append(contentsOf: group)
        }
        
        return ret
    }
    
    func assetToImage(_ asset: PHAsset, asThumbnail:Bool = false, completed:(@escaping (UIImage?) -> Void))
    {
        let manager = imgManager
        let option = PHImageRequestOptions()
        option.isSynchronous = false
        option.deliveryMode = .highQualityFormat
        option.version = .current
        option.resizeMode = .exact
        option.isNetworkAccessAllowed = true
        
        let dims:CGSize
            
        if (!asThumbnail || asset.pixelHeight == 0)
            { dims = CGSize(width: asset.pixelWidth, height: asset.pixelHeight)}
        else
        {
            let ratio = asset.pixelWidth / asset.pixelHeight
            let width = 120
            let height = width / ratio
            
            dims = CGSize(width: width, height: height)
        }
        
        manager.requestImage(for: asset, targetSize: dims, contentMode: .aspectFit, options: option, resultHandler: {(image: UIImage?, info:[AnyHashable : Any]?) -> Void in
                completed(image)
            }
        )
    }
    
    func assetToData(_ asset:PHAsset, completed:(@escaping (Data?) -> Void)) {
        let manager = imgManager
        let option = PHImageRequestOptions()
        option.isSynchronous = false
        option.deliveryMode = .highQualityFormat
        option.version = .current
        option.resizeMode = .exact
        option.isNetworkAccessAllowed = true
        
        manager.requestImageData(for: asset, options: option, resultHandler: { data, type, orientation, dict in
            completed(data)
        })
    }
    
    func assetToUrl(_ asset:PHAsset, completed:(@escaping (URL?) -> Void)) {
        let option = PHVideoRequestOptions()
        option.deliveryMode = .automatic
        option.isNetworkAccessAllowed = false
        
        imgManager.requestAVAsset(forVideo: asset, options: option, resultHandler: {(asset, audio, dict) in
            guard let asset = asset as? AVURLAsset else { completed(nil); return }
            
            completed(asset.url)
        })
    }
    
    func videoAssetPreviewImage() {
        func videoSnapshot(filePathLocal: String) -> UIImage? {

            let vidURL = URL(fileURLWithPath:filePathLocal as String)
            let asset = AVURLAsset(url: vidURL)
            let generator = AVAssetImageGenerator(asset: asset)
            generator.appliesPreferredTrackTransform = true

            let timestamp = CMTime(seconds: 1, preferredTimescale: 60)

            do {
                let imageRef = try generator.copyCGImage(at: timestamp, actualTime: nil)
                return UIImage(cgImage: imageRef)
            }
            catch let error as NSError
            {
                print("Image generation failed with error \(error)")
                return nil
            }
        }
    }
    
    func deleteAssets(_ assets:[PHAsset]) -> Bool
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
    
    public func saveImageToPhotos(dataPath:String, extension ext:String, onSaved: @escaping (String?) -> Void)
    {
        do
        {
            let url = URL(fileURLWithPath: dataPath)
            let data = try Data(contentsOf: url)
            saveImageToPhotos(data:data, extension: ext, onSaved: onSaved)
        }
        catch{
        }
    }
    
    let galleryTitle:String = Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as! String
    var placeholder:PHObjectPlaceholder? = nil
    
    func getGallery() -> PHAssetCollection?
    {
        let assets = PHAssetCollection.fetchAssetCollections(with: PHAssetCollectionType.album, subtype: PHAssetCollectionSubtype.albumRegular, options: nil)
        
        let sz = assets.count
        if (sz == 0)
            { return nil }
        
        for i in 0 ... sz - 1
        {
            let asset = assets.object(at: i)
            
            if (asset.localizedTitle == galleryTitle)
                { return asset }
        }
        
        return nil
    }
    
    func saveToPhotos2(_ item:Any)
    {
        let gallery = self.getGallery()
        var galleryReq:PHAssetCollectionChangeRequest
        if (gallery == nil)
        {
            galleryReq = PHAssetCollectionChangeRequest.creationRequestForAssetCollection(withTitle: self.galleryTitle)
        }
        else
        {
            galleryReq = PHAssetCollectionChangeRequest(for: gallery!)!
        }
        
        if item is Data,
           let data = item as? Data {
            let createRequest = PHAssetCreationRequest.forAsset()
            let options = PHAssetResourceCreationOptions()
            options.shouldMoveFile = true
            createRequest.addResource(with: PHAssetResourceType.photo, data: data, options: options)
            placeholder = createRequest.placeholderForCreatedAsset
        }
        else if item is URL,
                let url = item as? URL {
            let options = PHAssetResourceCreationOptions()
            options.shouldMoveFile = true
            let assetRequest = PHAssetCreationRequest.forAsset()
            assetRequest.addResource(with: .video, fileURL: url, options: options)
            placeholder = assetRequest.placeholderForCreatedAsset
        }
        
        let list = NSSet(object: placeholder as Any)
        galleryReq.addAssets(list)
    }
    
    func getFile(_ urlStr: String) -> PHAsset? {
        let queryItems = URLComponents(string: urlStr)?.queryItems
        guard let id = queryItems?.filter({$0.name == "id"}).first,
              let idStr = id.value
        else { return nil }
                
        let collectionResult = PHAsset.fetchAssets(withLocalIdentifiers: [idStr], options: nil)
        
        let sz = collectionResult.count
        guard sz > 0 else { return nil }
        
        let asset = collectionResult.object(at: 0)
        return asset
    }
    
    public func saveImageToPhotos(data:Data, extension ext:String, onSaved: @escaping (String?) -> Void)
    {
        saveAssetToPhotos(item: data, ext: ext, onSaved: onSaved)
    }
    
    public func saveVideoToPhotos(url:URL, onSaved: @escaping (String?) -> Void){
        saveAssetToPhotos(item: url, ext: "mov", onSaved: onSaved)
    }
    
    private func saveAssetToPhotos(item:Any, ext: String, onSaved: @escaping (String?) -> Void){
        PHPhotoLibrary.requestAuthorization {
        [unowned self, onSaved] status in
            guard status == .authorized else { return }
            PHPhotoLibrary.shared().performChanges({ self.saveToPhotos2(item) }, completionHandler: { success, err in
                if success {
                    // Clean up
                    if item is URL, let url = item as? URL,
                       FileManager.default.fileExists(atPath: url.path)
                    {
                        do {
                            try FileManager.default.removeItem(atPath: url.path)
                        } catch {
                            print("Could not remove file at url: \(url.path)")
                        }
                    }
                    
                    self.saveAssetToPhotos2(ext: ext, onSaved: onSaved)
                } else {
                    print (err)
                }
            })
        }
    }
    
    private func saveAssetToPhotos2(ext:String, onSaved: @escaping (String?) -> Void) {
        if (self.placeholder == nil)
            { return; }
        
        let localID = self.placeholder!.localIdentifier
        let assetID = localID.replacingOccurrences(of: "/.*", with: "", options: NSString.CompareOptions.regularExpression, range: nil)
        let assetURLStr = "assets-library://asset/asset.\(ext)?id=\(assetID)&ext=\(ext)"
        self.placeholder = nil
        
        onSaved(assetURLStr)
        guard let asset = getFile(assetURLStr) else { return }
        
        delegates.invoke { $0.onNewFile(asset: asset) }
    }
}

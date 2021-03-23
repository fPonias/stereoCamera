//
//  ImageSaver.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/22/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreImage
import Photos

public class ImageSaver
{
    public func saveToPhotos(data:Data, onSaved: @escaping (String?) -> Void)
    {
        PHPhotoLibrary.requestAuthorization {
        [unowned self, onSaved] status in
            guard status == .authorized else { return }
            do{ try PHPhotoLibrary.shared().performChangesAndWait({ self.saveToPhotos2(data) })}
                catch { return; }
            
            if (self.placeholder == nil)
                { return; }
            
            let localID = self.placeholder!.localIdentifier
            let assetID = localID.replacingOccurrences(of: "/.*", with: "", options: NSString.CompareOptions.regularExpression, range: nil)
            let ext = "jpg"
            let assetURLStr = "assets-library://asset/asset.\(ext)?id=\(assetID)&ext=\(ext)"
            self.placeholder = nil
            
            onSaved(assetURLStr)
        }
    }
    
    public func saveToPhotos(dataPath:String, onSaved: @escaping (String?) -> Void)
    {
        do
        {
            let url = URL(fileURLWithPath: dataPath)
            let data = try Data(contentsOf: url)
            saveToPhotos(data:data, onSaved: onSaved)
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
    
    func saveToPhotos2(_ data:Data)
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
        
        let createRequest = PHAssetCreationRequest.forAsset()
        createRequest.addResource(with: PHAssetResourceType.photo, data: data, options: nil)
        placeholder = createRequest.placeholderForCreatedAsset
        
        let list = NSSet(object: createRequest.placeholderForCreatedAsset as Any)
        galleryReq.addAssets(list)
    }
}

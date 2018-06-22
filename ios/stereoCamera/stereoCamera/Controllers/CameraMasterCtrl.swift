//
//  CameraMaster.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/17/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation
import GLKit
import Photos

class CameraMasterCtrl: UIViewController
{
    @IBOutlet weak var cameraPreview: CameraPreview!
    @IBOutlet weak var shutterBtn: UIButton!
    @IBOutlet weak var galleryBtn: UIButton!
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        let tmpUrl = Files.getTmpDir()
        let tmpUrlStr = tmpUrl.path
        let ptr = Bytes.toPointer(tmpUrlStr)
        imageProcessor_initN(ptr)
        
        cameraPreview.startCamera()
        
        openGallery(galleryBtn)
    }
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @IBAction func shutterFired(_ sender: Any)
    {
        if (cameraPreview.currentCamera == nil)
        {
            do
            {
                let data = NSDataAsset.init(name: "img107")
                let tmpUrl = self.saveToTmp(data: data!.data)
                guard (tmpUrl != nil) else { return }
                
                self.shutterFired2(tmpUrl!.path)
            }
            catch {}
            
            return
        }
        
        cameraPreview.fireShutter(delegate: {(photo: AVCapturePhoto) -> Void in
            let data = photo.fileDataRepresentation()
            guard ( data != nil ) else { return }
            
            let tmpUrl = self.saveToTmp(data: data!)
            guard (tmpUrl != nil) else { return }
            
            self.shutterFired2(tmpUrl!.path)
        })
    }
    
    func shutterFired2(_ path:String)
    {
        let ptr = Bytes.toPointer(path)
        
        imageProcessor_setProcessorType(Int32(SPLIT.rawValue))
        imageProcessor_setImageN(Int32(LEFT.rawValue), ptr, 0, 1.0)
        imageProcessor_setImageN(Int32(RIGHT.rawValue), ptr, 0, 1.0)
        
        let outurl = Files.getRandomFile()
        guard (outurl != nil) else { return }
        
        let outpath = outurl!.path
        let outptr = Bytes.toPointer(outpath)
        imageProcessor_processN(1, 0, outptr )
        
        imageProcessor_cleanUpN()
        
        saveToPhotos(dataPath: outpath)
    }
    
    static let galleryTitle:String = "3D Camera For 2"
    
    func saveToPhotos(dataPath:String)
    {
        PHPhotoLibrary.requestAuthorization { status in
            guard status == .authorized else { return }
            
            do
            {
                let url = URL(fileURLWithPath: dataPath)
                let data = try Data(contentsOf: url)
                
                PHPhotoLibrary.shared().performChanges({
                    let gallery = self.getGallery()
                    var galleryReq:PHAssetCollectionChangeRequest
                    if (gallery == nil)
                    {
                        galleryReq = PHAssetCollectionChangeRequest.creationRequestForAssetCollection(withTitle: CameraMasterCtrl.galleryTitle)
                    }
                    else
                    {
                        galleryReq = PHAssetCollectionChangeRequest(for: gallery!)!
                    }
                    
                    let createRequest = PHAssetCreationRequest.forAsset()
                    createRequest.addResource(with: PHAssetResourceType.photo, data: data, options: nil)
                    
                    let list = NSSet(object: createRequest.placeholderForCreatedAsset as Any)
                    galleryReq.addAssets(list)
                }, completionHandler: self.photoCompletion)
            }
            catch{
            }
        }
    }
    
    func getGallery() -> PHAssetCollection?
    {
        let assets = PHAssetCollection.fetchAssetCollections(with: PHAssetCollectionType.album, subtype: PHAssetCollectionSubtype.albumRegular, options: nil)
        
        let sz = assets.count
        if (sz == 0)
            { return nil }
        
        for i in 0 ... sz - 1
        {
            let asset = assets.object(at: i)
            
            if (asset.localizedTitle == CameraMasterCtrl.galleryTitle)
                { return asset }
        }
        
        return nil
    }
    
    func photoCompletion(completed:Bool, error:Error?)
    {
        
    }
    
    func saveToTmp(data: Data) -> URL?
    {
        let dt = Date()
        let rnd = arc4random_uniform(1000)
        var tmpUrl = Files.getTmpDir()
        let fileName = String(dt.timeIntervalSince1970.rounded()) + "-" + String(rnd) + ".jpg"
        tmpUrl = tmpUrl.appendingPathComponent(fileName)
        
        do
        {
            try data.write(to: tmpUrl)
        }
        catch {
            print ("Failed to save tmp jpg data to " + tmpUrl.path)
            return nil
        }
        
        return tmpUrl
    }
    
    @IBAction func openGallery(_ sender: Any)
    {
        performSegue(withIdentifier: "MasterToGallery", sender: self)
    }
}

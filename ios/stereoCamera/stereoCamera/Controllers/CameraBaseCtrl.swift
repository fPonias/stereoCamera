//
//  File.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/24/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit
import Photos

class CameraBaseCtrl : UIViewController
{
    var zoomSlider:UISlider
    {
        get { return UISlider() }
    }
    
    var cameraPreview:CameraPreview
    {
        get { return CameraPreview() }
    }

    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        zoomSlider.maximumValue = 4.0
        zoomSlider.minimumValue = 1.0
        zoomSlider.value = 1.0
        
        let tmpUrl = Files.getTmpDir()
        let tmpUrlStr = tmpUrl.path
        let ptr = Bytes.toPointer(tmpUrlStr)
        imageProcessor_initN(ptr)
        
        NotificationCenter.default.addObserver(self, selector: #selector(CameraBaseCtrl.rotated), name: NSNotification.Name.UIDeviceOrientationDidChange, object: nil)
    }
    
    @objc func rotated()
    {
        cameraPreview.updateTransform()
    }
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    let alert = UIAlertController(title: nil, message: "Camera Busy", preferredStyle: UIAlertControllerStyle.alert)
    var isShowing = false
    var loadingIndicator: UIActivityIndicatorView? = nil
    
    func showLoader(_ show:Bool)
    {
        if (show == isShowing)
            { return }
        
        isShowing = show
    
        DispatchQueue.main.async {
        [unowned self] in
            if (show)
            {
                self.loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 10, y: 5, width: 50, height: 50 ))
                self.loadingIndicator!.hidesWhenStopped = true
                self.loadingIndicator!.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.gray
                self.loadingIndicator!.startAnimating()
                
                self.alert.view.addSubview(self.loadingIndicator!)
                self.present(self.alert, animated: true, completion: nil)
            }
            else
            {
                self.alert.dismiss(animated: false, completion: nil)
            }
        }
    }
    
    func setLoadingMessage(_ message:String)
    {
        alert.message = message
    }
    
    let galleryTitle:String = Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as! String
    
    func saveToPhotos(data:Data)
    {
        PHPhotoLibrary.requestAuthorization {
        status in
            guard status == .authorized else { return }
            PHPhotoLibrary.shared().performChanges({ self.saveToPhotos2(data) }, completionHandler: self.photoCompletion)
            
        }
    }
    
    func saveToPhotos(dataPath:String)
    {
        do
        {
            let url = URL(fileURLWithPath: dataPath)
            let data = try Data(contentsOf: url)
            saveToPhotos(data:data)
        }
        catch{
        }
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
        
        let list = NSSet(object: createRequest.placeholderForCreatedAsset as Any)
        galleryReq.addAssets(list)
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
            
            if (asset.localizedTitle == galleryTitle)
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
        let fileName = String(Int(dt.timeIntervalSince1970)) + "-" + String(rnd) + ".jpg"
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
    
    var status = Status.NONE
    
    func setStatus(_ status:Status)
    {
        self.status = status

        switch (status)
        {
            case .BUSY:
                setLoadingMessage("Camera busy")
                showLoader(true)
            case .CREATED, .RESUMED:
                setLoadingMessage("Preview created")
                showLoader(true)
            case .LISTENING:
                setLoadingMessage("Setting up communication")
                showLoader(true)
            case .PROCESSING:
                setLoadingMessage("Processing photo data")
                showLoader(true)
            case .READY:
                setLoadingMessage("Ready")
                showLoader(false)
            default:
                break
        }
    }
}

enum Status:Int
{
    case NONE,
    CREATED,
    RESUMED,
    LISTENING,
    PROCESSING,
    READY,
    BUSY
}


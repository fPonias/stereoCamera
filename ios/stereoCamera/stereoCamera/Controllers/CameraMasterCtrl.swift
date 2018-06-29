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

class CameraMasterCtrl: CameraBaseCtrl
{
    @IBOutlet weak var _cameraPreview: CameraPreview!
    @IBOutlet weak var shutterBtn: UIButton!
    @IBOutlet weak var galleryBtn: UIButton!
    @IBOutlet weak var _zoomSlider: UISlider!
    
    override var cameraPreview: CameraPreview
    {
        get { return _cameraPreview }
    }
    
    override var zoomSlider: UISlider
    {
        get { return _zoomSlider }
    }
    
    @IBAction func zoomChanged(_ sender: Any)
    {
        let current = zoomSlider .value
        cameraPreview.zoom = current
    }
    
    let masterShake:MasterShake = MasterShake()
    let slaveState:SlaveState = SlaveState()
    let resumed = false
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?)
    {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        masterShake.parent = self //how to work this into an initializer?  beats me
    }
    
    required init?(coder aDecoder: NSCoder)
    {
        super.init(coder: aDecoder)
        masterShake.parent = self
    }
    
    class SlaveListener : SlaveStateListener
    {
        unowned var parent:CameraMasterCtrl
    
        init(parent:CameraMasterCtrl)
        {
            self.parent = parent
        }
    
        func onStatus(status: Status)
        {
            if (status == .RESUMED && parent.resumed)
            {
                if (!(parent.status == Status.CREATED || parent.status == Status.LISTENING))
                {
                    parent.handshake();
                }
                else
                {
                    parent.pauseConnection();
                    parent.handshake();
                }
            }
        }
        
        func onZoom(zoom: Float) {}
        
        func onFov(horiz: Float, vert: Float) {}
        
        func onGravity(gravity: Any) {}
        
        func onOrientation(orientation: UIDeviceOrientation) {}
        
        func onConnectionPause() {}
        
        func onDisconnect() {}
        
        func onPreviewFrame(data: [UInt8], zoom: Float) {}
    }
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        slaveState.listener = SlaveListener(parent: self)
        slaveState.start()
        
        showLoader(true)
        
        DispatchQueue.global(qos: .userInitiated).async
        {
            usleep(5000000)
            self.handshake()
        }
    }
    
    func handshake()
    {
        self.masterShake.start(listener: {(success: Bool) -> Void
        in
            DispatchQueue.main.async
            {
                if (!success)
                {
                    print("master handshake failed")
                    self.navigationController?.popViewController(animated: true)
                }
                else
                {
                    self.showLoader(false)
                }
            }
        })
    }
    
    func pauseConnection()
    {
        //if (slavePreview != null)
        //    slavePreview.cancel();

        //if (previewView != null)
        //    previewView.stopPreview();

        if ((status == .READY || status == .BUSY) && (slaveState.status == .READY || slaveState.status == .BUSY))
            { /*masterComm.runCommand(new ConnectionPause(), null); */ }

        setStatus(.CREATED);
    }
    
    func disconnect()
    {
        pauseConnection();
        //MainActivity.getInstance().cleanUpConnections();
        self.navigationController?.popViewController(animated: true)
    }
    
    private let shutterLock = NSCondition()
    private var shutterEvents = 0
    private var shutterLocal:String = ""
    private var shutterRemote:String = ""
    
    @IBAction func shutterFired(_ sender: Any)
    {
        var doReturn = false
        shutterLock.lock()
            if (shutterEvents != 0)
                { doReturn = true }
            else
            {
                shutterEvents = 3
                shutterLocal = ""
                shutterRemote = ""
            }
        shutterLock.unlock()
        
        if (doReturn)
            { return }
    
        showLoader(true)
        
        shutterFireLocal()
        shutterFireRemote()
        
        DispatchQueue.global(qos: .userInitiated).async
        {
            while (self.shutterEvents > 1)
            {
                self.shutterLock.lock()
                    if (self.shutterEvents > 1)
                        { self.shutterLock.wait() }
                self.shutterLock.unlock()
            }
            
            self.shutterFired2()
        }
    }
    
    func shutterFireLocal()
    {
        if (self.cameraPreview.currentCamera == nil)
        {
            DispatchQueue.global(qos: .userInitiated).async(execute:
            {
                let data = NSDataAsset.init(name: "img107")
                let tmpUrl = self.saveToTmp(data: data!.data)
                guard (tmpUrl != nil) else { self.shutterReset(); return  }
                
                self.shutterLock.lock()
                    self.shutterEvents -= 1
                    self.shutterLocal = tmpUrl!.path
                    self.shutterLock.signal()
                self.shutterLock.unlock()
            })
            return
        }
        else
        {
            DispatchQueue.global(qos: .userInitiated).async
            {
                self.cameraPreview.fireShutter(delegate: {(photo: AVCapturePhoto) -> Void
                in
                    let data = photo.fileDataRepresentation()
                    guard ( data != nil ) else { self.shutterReset();  return }
                    
                    let tmpUrl = self.saveToTmp(data: data!)
                    guard (tmpUrl != nil) else { self.shutterReset(); return }
                    
                    self.shutterLock.lock()
                        if (self.shutterEvents > 0)
                        {
                            self.shutterEvents -= 1
                            self.shutterLocal = tmpUrl!.path
                            self.shutterLock.signal()
                        }
                    self.shutterLock.unlock()
                })
            }
        }
    }
    
    func shutterFireRemote()
    {
        let cmd = FireShutter()
        CommManager.instance.comm.sendCommand(command: cmd, listener: {(origCmd:Command?, newCmd:Command) -> Void
        in
            let dataCmd = newCmd as! FireShutter
            guard (dataCmd.data.count > 0) else { self.shutterReset(); return }
            
            let tmpUrl = self.saveToTmp(data: Data(dataCmd.data))
            guard (tmpUrl != nil) else { self.shutterReset(); return }
            
            self.shutterLock.lock()
                if (self.shutterEvents > 0)
                {
                    self.shutterEvents -= 1
                    self.shutterRemote = tmpUrl!.path
                    self.shutterLock.signal()
                }
            self.shutterLock.unlock()
        })
    }
    
    func shutterReset()
    {
        self.shutterLock.lock()
            self.shutterEvents = -1
            self.shutterLocal = ""
            self.shutterRemote = ""
            self.shutterLock.signal()
        self.shutterLock.unlock()
    }
    
    func shutterFired2()
    {
        var doReturn = false
        self.shutterLock.lock()
            if (self.shutterEvents < 1)
            {
                doReturn = true
                self.shutterEvents = 0
                self.shutterLocal = ""
                self.shutterRemote = ""
            }
        self.shutterLock.unlock()
        
        if (doReturn)
            { return }
    
        let localPtr = Bytes.toPointer(self.shutterLocal)
        let remotePtr = Bytes.toPointer(self.shutterRemote)
        
        imageProcessor_setProcessorType(Int32(SPLIT.rawValue))
        imageProcessor_setImageN(Int32(LEFT.rawValue), localPtr, 0, 1.0)
        imageProcessor_setImageN(Int32(RIGHT.rawValue), remotePtr, 0, 1.0)
        
        let outurl = Files.getRandomFile()
        guard (outurl != nil) else { shutterReset(); return }
        
        let outpath = outurl!.path
        let outptr = Bytes.toPointer(outpath)
        imageProcessor_processN(1, 0, outptr )
        
        imageProcessor_cleanUpN()
        showLoader(false)
        
        saveToPhotos(dataPath: outpath)
        sendProcessedPhoto(dataPath: outpath)
        
        self.shutterLock.lock()
            self.shutterEvents = 0
            self.shutterLocal = ""
            self.shutterRemote = ""
        self.shutterLock.unlock()
    }
    
    func sendProcessedPhoto(dataPath:String)
    {
        do
        {
            let url = URL(fileURLWithPath: dataPath)
            let data = try Data(contentsOf: url)
            
            let cmd = SendPhoto(dta: data)
            CommManager.instance.comm.sendCommand(command: cmd)
        }
        catch {
        }
    }
    
    @IBAction func openGallery(_ sender: Any)
    {
        performSegue(withIdentifier: "MasterToGallery", sender: self)
    }
}

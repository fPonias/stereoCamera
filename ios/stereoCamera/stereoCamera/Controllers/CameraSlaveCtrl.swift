//
//  CameraSlaveCtrl.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/24/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit
import AVKit

class CameraSlaveCtrl : CameraBaseCtrl, CommandListener
{
    @IBOutlet weak var _cameraPreview: CameraPreview!
    @IBOutlet weak var _zoomSlider: UISlider!
    @IBOutlet weak var galleryBtn: GalleryBtn!
    
    var pingReceived = false;
    
    func onCommand(_ command: Command)
    {
        switch (command.cmdtype)
        {
        case CommandTypes.PING:
            if (!command.isResponse) //prevent infinite feedback
            {
                let ping = Ping()
                ping.id = command.id
                ping.isResponse = true
                CommManager.instance.comm.sendCommand(command: ping)
            }
        case CommandTypes.SEND_VERSION:
            let version = Version()
            version.id = command.id
            version.isResponse = true
            CommManager.instance.comm.sendCommand(command: version)
        case CommandTypes.HANDSHAKE:
            self.cancelConnection()
            self.showLoader(true)
            self.pingReceived = true;

            self.setStatus(Status.LISTENING);
            
            let handshake = Handshake()
            handshake.id = command.id
            handshake.isResponse = true
            CommManager.instance.comm.sendCommand(command: handshake)
        case CommandTypes.SET_OVERLAY:
            let setOverlay = SetOverlay((command as! SetOverlay).overlay)
            setOverlay.id = command.id
            setOverlay.isResponse = true
            CommManager.instance.comm.sendCommand(command: setOverlay)
            self.showLoader(false)
        case CommandTypes.SET_FACING:
            let setFacing = SetFacing((command as! SetFacing).facing)
            setFacing.id = command.id
            setFacing.isResponse = true
            CommManager.instance.comm.sendCommand(command: setFacing)
            
            cameraPreview.startCamera()
        case CommandTypes.SET_ZOOM:
            let setZoom = SetZoom((command as! SetZoom).zoom)
            setZoom.id = command.id
            setZoom.isResponse = true
            CommManager.instance.comm.sendCommand(command: setZoom)
            
            DispatchQueue.main.async {
            [ unowned self ] in
                self.zoomSlider.setValue(setZoom.zoom, animated: false)
                self.zoomChanged(self.zoomSlider)
            }
            
        case CommandTypes.FIRE_SHUTTER:
            self.fireShutter(listener: {(data:Data?) -> Void
            in
                let fireShutter = FireShutter()
                
                if (data != nil)
                    { fireShutter.data = [UInt8](data!) }
                
                fireShutter.id = command.id
                fireShutter.isResponse = true
                CommManager.instance.comm.sendCommand(command: fireShutter)
            })
        case CommandTypes.SEND_PROCESSED_PHOTO:
            let sendPhoto = command as! SendPhoto
            let url = saveToTmp(data: Data(sendPhoto.data))
            
            if (url != nil)
            {
                saveToPhotos(dataPath: url!.path)
                
                DispatchQueue.main.async {
                [ unowned self ] in
                    self.galleryBtn.update()
                }
            }
        default:
            print("unsupported command " + command.cmdtype.description)
        }
    }
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        print ("slave view loaded.  creating command listener")
        CommManager.instance.comm.commandListener = self
        
        DispatchQueue.global(qos: .userInitiated).async
        { [unowned self] in
            usleep(500000)
            self.setStatus(Status.CREATED)
        }
        
        showLoader(true)
        
        galleryBtn.update()
    }
    
    override var cameraPreview: CameraPreview
    {
        get { return _cameraPreview }
    }
    
    override var zoomSlider: UISlider
    {
        get { return _zoomSlider }
    }
    
    var currentZoom:Float = 1.0
    var zoomSending = false
    let zoomSendDelay:UInt32 = 100000
    
    @IBAction func zoomChanged(_ sender: Any)
    {
        currentZoom = zoomSlider .value
    
        //avoid spamming zoom updates during the slider drag
        if (!zoomSending)
        {
            zoomSending = true
            DispatchQueue.global(qos: .background).async {
            [unowned self] in
                self.zoomChanged2()
            }
        }
        
        cameraPreview.zoom = currentZoom
    }
    
    private func zoomChanged2()
    {
        usleep(zoomSendDelay)
        
        let cmd = SendZoom(currentZoom)
        CommManager.instance.comm.sendCommand(command: cmd)
        zoomSending = false
    }
    
    @IBAction func openGallery(_ sender: Any)
    {
        performSegue(withIdentifier: "SlaveToGallery", sender: self)
    }
    
    func cancelConnection()
    {
        pingReceived = false;
        //previewSender.cancel();
        //stopPreview();

        //if (status == Status.READY || status == Status.BUSY)
        //    slaveComm.sendCommand(new SendConnectionPause());

        setStatus(Status.CREATED);
    }
    
    override func setStatus(_ status: Status)
    {
        let sendStatus = SendStatus(status)
        CommManager.instance.comm.sendCommand(command: sendStatus)
    }
    
    func fireShutter(listener: @escaping DataListener)
    {
        if (self.cameraPreview.currentCamera == nil)
        {
            DispatchQueue.global(qos: .userInitiated).async(execute:
            {
                let data = NSDataAsset.init(name: "img107")
                guard (data != nil) else { listener(nil); return  }
                
                listener(data!.data)
            })
        }
        else
        {
            DispatchQueue.global(qos: .userInitiated).async
            {
                self.cameraPreview.fireShutter(delegate: {(photo: AVCapturePhoto) -> Void
                in
                    let data = photo.fileDataRepresentation()
                    listener(data)
                })
            }
        }
    }
}

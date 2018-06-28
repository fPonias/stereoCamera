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
        case CommandTypes.SET_FACING:
            let setFacing = SetFacing((command as! SetFacing).facing)
            setFacing.id = command.id
            setFacing.isResponse = true
            CommManager.instance.comm.sendCommand(command: setFacing)
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
    }
    
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

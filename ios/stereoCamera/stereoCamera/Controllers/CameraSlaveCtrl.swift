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
import CoreMotion

class CameraSlaveCtrl : CameraBaseCtrl, CommandListener
{
    @IBOutlet weak var _cameraPreview: CameraPreview!
    @IBOutlet weak var _zoomSlider: UISlider!
    @IBOutlet weak var galleryBtn: GalleryBtn!
    @IBOutlet weak var cameraOverlay: CameraPreviewOverlay!
    
    var pingReceived = false;
    var cameraPosition = AVCaptureDevice.Position.back
    var captureQuality = ImageQuality.LOW
    
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
            
            setStatus(.READY)
            
            cameraOverlay.overlay = setOverlay.overlay
        case CommandTypes.SET_FACING:
            let setFacing = SetFacing((command as! SetFacing).facing)
            setFacing.id = command.id
            setFacing.isResponse = true
            CommManager.instance.comm.sendCommand(command: setFacing)
            
            let newPos = (setFacing.facing) ? AVCaptureDevice.Position.front : AVCaptureDevice.Position.back;
            
            cameraPosition = newPos
            cameraPreview.stopCamera()
            cameraPreview.startCamera(cameraPosition: newPos, quality: captureQuality)
        case CommandTypes.SET_CAPTURE_QUALITY:
            let retCmd = SetCaptureQuality((command as! SetCaptureQuality).quality)
            retCmd.id = command.id
            retCmd.isResponse = true
            CommManager.instance.comm.sendCommand(command: retCmd)
            
            captureQuality = retCmd.quality
            cameraPreview.stopCamera()
            cameraPreview.startCamera(cameraPosition: cameraPosition, quality: captureQuality)
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
            self.fireShutter(listener: {[unowned self] (data:Data?) -> Void
            in
                let fireShutter = FireShutter()
                
                if (data != nil)
                    { fireShutter.data = [UInt8](data!) }
                
                fireShutter.id = command.id
                fireShutter.orientation = self.cameraPreview.getOrientation()
                fireShutter.zoom = self.currentZoom
                fireShutter.isResponse = true
                
                class Foo : CommandResponseListener
                {
                    weak var parent:CameraSlaveCtrl?
                    init(_ parent:CameraSlaveCtrl)
                    {
                        self.parent = parent
                    }
                
                    override func onSent(_ command: Command)
                    {
                        parent?.setLoaderMessage("Receiving Processed Photo")
                    }
                }
                
                CommManager.instance.comm.sendCommand(command: fireShutter)
            })
        case CommandTypes.SEND_PROCESSED_PHOTO:
            let sendPhotoReply = SendPhoto()
            sendPhotoReply.id = command.id
            sendPhotoReply.isResponse = true
            CommManager.instance.comm.sendCommand(command: sendPhotoReply)
            
            let sendPhoto = command as! SendPhoto
            let url = saveToTmp(data: Data(sendPhoto.data))
            
            if (url != nil)
            {
                saveToPhotos(dataPath: url!.path, onSaved: {
                (_ path:String?) in
                    self.showLoader(false)
                    
                    DispatchQueue.main.async {
                        [ unowned self ] in
                        self.galleryBtn.update()
                    }
                })
            }
        case CommandTypes.DISCONNECT:
            self.disconnect()
        case CommandTypes.CONNECTION_PAUSE:
            self.cancelConnection()
        case CommandTypes.LATENCY_CHECK:
            let cmd = command as! LatencyTest
            self.runSync(command: cmd)
        case CommandTypes.ID:
            let cmd = command as! ID
            cmd.phoneId = Cookie.instance.id
            cmd.isResponse = true
            CommManager.instance.comm.sendCommand(command: cmd)
        default:
            print("unsupported command " + command.cmdtype.description)
        }
    }
    
    func onDisconnect()
    {
        disconnect()
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
        
        galleryBtn.setNavigationController(ctrl: navigationController)
        galleryBtn.update()
    }
    
    override func viewWillAppear(_ animated: Bool)
    {
        super.viewWillAppear(animated)
        
        DispatchQueue.global(qos: .userInitiated).async
        { [unowned self] in
            usleep(500000)
            self.setStatus(.RESUMED)
        }
    }
    
    override func viewWillDisappear(_ animated: Bool)
    {
        super.viewWillDisappear(animated)
    
        let idx = navigationController?.viewControllers.index(of: self)
        let sz = navigationController?.viewControllers.count
        if (idx == nil)
        {
            let cmd = Disconnect()
            CommManager.instance.comm.sendCommand(command: cmd)
            CommManager.instance.comm.disconnect()
        }
        else if (idx! < sz! - 1)
        {
            let cmd = ConnectionPause()
            CommManager.instance.comm.sendCommand(command: cmd)
            setStatus(.BUSY)
        }
    }
    
    override func gravityHandler(data: CMAccelerometerData?, error: Error?)
    {
        if (data == nil)
            { return }

        let value = Gravity(x: Float(data!.acceleration.x), y: Float(data!.acceleration.y), z: Float(data!.acceleration.z))
        let cmd = SendGravity(gravity: value)
        CommManager.instance.comm.sendCommand(command: cmd)
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
        currentZoom = zoomSlider.value
    
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
    
    override func onConnectCancelled()
    {
        disconnect()
    }
    
    func cancelConnection()
    {
        pingReceived = false;
        //previewSender.cancel();

        if (status == Status.READY || status == Status.BUSY)
            { /*CommManager.instance.comm.sendCommand(command: SendConnectionPause())*/ }

        setStatus(Status.BUSY);
    }
    
    override func setStatus(_ status: Status)
    {
        super.setStatus(status)
        let sendStatus = SendStatus(status)
        CommManager.instance.comm.sendCommand(command: sendStatus)
    }
    
    func fireShutter(listener: @escaping DataListener)
    {
        showLoader(true, message: "Taking Picture ...")
    
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
                self.cameraPreview.fireShutter(delegate: {[unowned self] (photo: AVCapturePhoto) -> Void
                in
                    let data = photo.fileDataRepresentation()
                    self.setLoaderMessage("Sending Picture ...")
                    listener(data)
                })
            }
        }
    }
    
    func runSync(command:LatencyTest)
    {
        let start = Date()
        DispatchQueue.global(qos: .userInitiated).async
        {
            self.cameraPreview.fireShutter(delegate: {(photo: AVCapturePhoto) -> Void
            in
                let result = Int((Date().timeIntervalSince1970 - start.timeIntervalSince1970) * 1000.0)
                command.elapsed = result
                command.isResponse = true
                
                CommManager.instance.comm.sendCommand(command: command)
            })
        }
    }
    
    @IBAction func openFaq(_ sender: Any)
    {
        performSegue(withIdentifier: "SlaveCameraFAQSegue", sender: self)
    }
}

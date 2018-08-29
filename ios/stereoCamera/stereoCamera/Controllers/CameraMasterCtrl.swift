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
import CoreMotion

class CameraMasterCtrl: CameraBaseCtrl
{
    @IBOutlet weak var _cameraPreview: CameraPreview!
    @IBOutlet weak var cameraPreviewOverlay: CameraPreviewOverlay!
    @IBOutlet weak var shutterBtn: UIButton!
    @IBOutlet weak var _zoomSlider: UISlider!
    @IBOutlet weak var handPhoneBtn: UIButton!
    @IBOutlet weak var galleryBtn: GalleryBtn!
    @IBOutlet weak var settingsBtn: UIBarButtonItem!
    @IBOutlet weak var flipBtn: UIBarButtonItem!
    
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
        Cookie.instance.setZoomForClient(zoom: current, isMaster: true, client: CommManager.instance.comm.address, camera: cameraPreview.currentCamera!.position)
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
    
    override func viewWillDisappear(_ animated: Bool)
    {
        super.viewWillDisappear(animated)
    
        let idx = navigationController?.viewControllers.index(of: self)
        if (idx == nil)
        {
            let cmd = Disconnect()
            CommManager.instance.comm.sendCommand(command: cmd)
            CommManager.instance.comm.disconnect()
        }
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
        
        func onZoom(zoom: Float)
        {
            Cookie.instance.setZoomForClient(zoom: zoom, isMaster: false, client: CommManager.instance.comm.address, camera: parent.cameraPreview.currentCamera!.position)
        }
        
        func onFov(horiz: Float, vert: Float) {}
        
        func onGravity(gravity: Gravity)
        {
            let horiz = Angles.horizontalOrientation(orientation: UIDevice.current.orientation, gravity: gravity)
            //parent.levelMeter.rotation2 = horiz
            
            let vert = Angles.verticalOrientation(gravity: gravity)
            //parent.tiltMeter.rotation2 = vert
        }
        
        func onOrientation(orientation: UIDeviceOrientation) {}
        
        func onConnectionPause() {}
        
        func onDisconnect()
        {
            parent.disconnect()
        }
        
        func onPreviewFrame(data: [UInt8], zoom: Float) {}
    }
    
    override func gravityHandler(data:CMAccelerometerData?, error:Error?)
    {
        if (data == nil)
            { return }
        
        let gravity = Gravity(x: Float(data!.acceleration.x), y: Float(data!.acceleration.y), z: Float(data!.acceleration.z))
        let horiz = Angles.horizontalOrientation(orientation: UIDevice.current.orientation, gravity: gravity)
        //levelMeter.rotation1 = horiz
    
        let vert = Angles.verticalOrientation(gravity: gravity)
        //tiltMeter.rotation1 = vert
    }
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        slaveState.listener = SlaveListener(parent: self)
        slaveState.start()
        
        showLoader(true)
        
        let cam = Cookie.instance.camera
        let quality = Cookie.instance.imageQuality
        cameraPreview.startCamera(cameraPosition: cam, quality: quality)
        cameraPreviewOverlay.overlay = Cookie.instance.overlay
        
        DispatchQueue.global(qos: .userInitiated).async
        {
            usleep(1500000)
            self.handshake()
        }
        
        galleryBtn.setNavigationController(ctrl: navigationController)
        galleryBtn.update()
        updateTriggerLayout()
    }
    
    func handshake()
    {
        showLoader(true)
        self.masterShake.start(listener: {[unowned self] (success: Bool) -> Void
        in
            DispatchQueue.main.async
            {
                self.showLoader(false, message: "Loading ...")
                if (!success)
                {
                    print("master handshake failed")
                    self.navigationController?.popViewController(animated: true)
                }
            }
            
            if (Cookie.instance.runSyncTest)
            {
                Cookie.instance.runSyncTest = false
                self.runSync()
            }
        })
    }
    
    override func onConnectCancelled()
    {
        disconnect()
    }
    
    func pauseConnection()
    {
        cameraPreview.stopCamera()
    
        if ((status == .READY || status == .BUSY) && (slaveState.status == .READY || slaveState.status == .BUSY))
        {
            let command = ConnectionPause()
            CommManager.instance.comm.sendCommand(command: command)
            
            showLoader(true)
        }

        setStatus(.CREATED);
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
    
        showLoader(true, message: "Taking pictures ...")
        Cookie.instance.camera = cameraPreview.currentCamera!.position
        
        var delay = 0
        if (Cookie.instance.useSync)
            { delay = calculateSyncDelay() }
        
        
        shutterFireLocal(delay: delay)
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
    
    private func calculateSyncDelay() -> Int
    {
        let localStats = Cookie.instance.getLocalSync(id: slaveState.id)
        let remoteStats = Cookie.instance.getRemoteSync(id: slaveState.id)
        
        if (localStats.count >= 2)
        {
            let result = Statistics.leastSquares(x: localStats, y: remoteStats)
            return Int(max(result.intercept, 0))
        }
        else
            { return 0 }
    }
    
    func shutterFireLocal(delay:Int)
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
                    self.setLoaderMessage("Waiting for remote picture ...")
                    self.shutterLock.signal()
                self.shutterLock.unlock()
            })
            return
        }
        else
        {
            DispatchQueue.global(qos: .userInitiated).async
            {
                if (delay > 0)
                    { usleep(UInt32(delay)) }
            
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
        CommManager.instance.comm.sendCommand(command: cmd, listenerFunc: {[unowned self](success:Bool, newCmd:Command, origCmd:Command?) -> Void
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
    
        setLoaderMessage("Combining pictures ...")
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
        
        saveToPhotos(dataPath: outpath)
        sendProcessedPhoto(dataPath: outpath)
        
        self.shutterLock.lock()
            self.shutterEvents = 0
            self.shutterLocal = ""
            self.shutterRemote = ""
        self.shutterLock.unlock()
        
        DispatchQueue.main.async {
        [ unowned self ] in
            self.galleryBtn.update()
        }
    }
    
    func sendProcessedPhoto(dataPath:String)
    {
        do
        {
            let url = URL(fileURLWithPath: dataPath)
            let data = try Data(contentsOf: url)
            
            let cmd = SendPhoto(dta: data)
            setLoaderMessage("Sending finished picture ...")
            CommManager.instance.comm.sendCommand(command: cmd, listenerFunc: {[ unowned self ] (_success:Bool, _ resp:Command, _ cmd:Command?) -> Void
            in
                self.showLoader(false)
            })
        }
        catch {
        }
    }
    
    @IBAction func openSettings(_ sender: Any)
    {
        performSegue(withIdentifier: "SettingsSegue", sender: self)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        //why the hell does this code go in the source controller?
        if (segue.identifier == "SettingsSegue")
        {
            guard let destCtrl = segue.destination as? SettingsCtrl else { return }
            destCtrl.setSettingsDoneHandler {
            [ unowned self ] in
                let overlay = Cookie.instance.overlay
                self.cameraPreviewOverlay.overlay = overlay
                
                self.pauseConnection()
                self.cameraPreview.startCamera(cameraPosition: Cookie.instance.camera, quality: Cookie.instance.imageQuality)
                self.handshake()
            }
        }
    }
    
    @IBAction func flipCamera(_ sender: Any)
    {
        pauseConnection()
        
        let curPos = cameraPreview.currentCamera?.position
        
        var newPos = AVCaptureDevice.Position.back
        if (curPos == .unspecified || curPos == .back)
        {
            newPos = AVCaptureDevice.Position.front
        }
        
        cameraPreview.startCamera(cameraPosition: newPos, quality: Cookie.instance.imageQuality)
        handshake()
    }
    
    @IBAction func switchHand(_ sender: Any)
    {
        let side = (Cookie.instance.side == LEFT) ? RIGHT : LEFT
        Cookie.instance.side = side
        
        updateTriggerLayout()
    }
    
    private func updateTriggerLayout()
    {
        let side = Cookie.instance.side
        
        if (side == LEFT)
        {
            shutterLeft.constant = 100
            shutterRight.constant = view.frame.width - 100 - shutterBtn.frame.width
            handBtnLeft.constant = view.frame.width - 30 - handPhoneBtn.frame.width
            handBtnRight.constant = 30
            
            handPhoneBtn.setImage(UIImage(named: "hand_phone"), for: .normal)
        }
        else
        {
            shutterRight.constant = 100
            shutterLeft.constant = view.frame.width - 100 - shutterBtn.frame.width
            handBtnRight.constant = view.frame.width - 30 - handPhoneBtn.frame.width
            handBtnLeft.constant = 30
            
            handPhoneBtn.setImage(UIImage(named: "hand_phone_right"), for: .normal)
        }
    }
    
    @IBOutlet weak var handBtnLeft: NSLayoutConstraint!
    @IBOutlet weak var handBtnRight: NSLayoutConstraint!
    @IBOutlet weak var shutterLeft: NSLayoutConstraint!
    @IBOutlet weak var shutterRight: NSLayoutConstraint!
    
    private struct syncStr
    {
        var start:Date
        var local:Int
        var remote:Int
        var delay:Int
    }
    
    private var syncCount:Int = 0
    
    private func runSync(delay:Int = 0)
    {
        if (syncCount == 0)
        {
            syncCount = 10
        }
    
        var result = syncStr(start: Date(), local: -1, remote: -1, delay: delay)
        let cmd = LatencyTest()
        
        DispatchQueue.global(qos: .userInitiated).async
        {
            usleep(UInt32(delay * 1000))
            self.cameraPreview.fireShutter(delegate: {[unowned self](photo: AVCapturePhoto) -> Void
            in
                result.local = Int((Date().timeIntervalSince1970 - result.start.timeIntervalSince1970) * 1000.0)
                
                if (result.local > -1 && result.remote > -1)
                {
                    self.runSync2(result)
                }
            })
        }
        
        CommManager.instance.comm.sendCommand(command: cmd, listenerFunc: { [unowned self] (success:Bool, newCmd:Command, origCmd:Command?) -> Void
        in
            let myCmd:LatencyTest = newCmd as! LatencyTest
            result.remote = myCmd.elapsed
            
            if (result.local > -1 && result.remote > -1)
            {
                self.runSync2(result)
            }
        })
    }
    
    private func runSync2(_ result: syncStr)
    {
        let diff = result.remote - result.local
        var remote = Cookie.instance.getRemoteSync(id: slaveState.id)
        remote.append(diff)
        Cookie.instance.setRemoteSync(remote, id: slaveState.id)
        
        var local = Cookie.instance.getLocalSync(id: slaveState.id)
        local.append(result.delay)
        Cookie.instance.setLocalSync(local, id: slaveState.id)
        
        let delay = max(0, result.delay + diff)
        syncCount -= 1
        if (syncCount > 0)
            { runSync(delay: delay) }
    }
}

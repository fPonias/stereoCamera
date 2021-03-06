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
    var resumed = false
    
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
        else
        {
            let cmd = ConnectionPause()
            CommManager.instance.comm.sendCommand(command: cmd)
        }
        
        showLoader(false)
        cameraPreview.stopCamera()
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
        
        func onConnectionPause()
        {
            parent.pauseConnection()
        }
        
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
            usleep(3000000)
            self.handshake()
        }
        
        galleryBtn.setNavigationController(ctrl: navigationController)
        galleryBtn.update()
        
        safeAreaObj = previewLeft.secondItem
        
        updateTriggerLayout()
        resumed = true
    }
    
    func handshake()
    {
        DispatchQueue.main.async
        {
            self.showLoader(true)
        }
        
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
        
        self.setStatus(.CREATED);
    }
    
    struct ShutterStruct
    {
        public var zoom:Float = 1.0
        public var orientation:CameraPreview.CameraOriention = .DEG_0
        public var tmpPath:String = ""
    }
    
    private let shutterLock = NSCondition()
    private var shutterEvents = 0
    private var shutterLocal = ShutterStruct()
    private var shutterRemote = ShutterStruct()
    
    @IBAction func shutterFired(_ sender: Any)
    {
        var doReturn = false
        shutterLock.lock()
            if (shutterEvents != 0)
                { doReturn = true }
            else
            {
                shutterEvents = 3
                shutterLocal = ShutterStruct()
                shutterRemote = ShutterStruct()
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
            { [unowned self] in
                let data = NSDataAsset.init(name: "img107")
                let tmpUrl = self.saveToTmp(data: data!.data)
                guard (tmpUrl != nil) else { self.shutterReset(); return  }
                
                self.shutterLock.lock()
                    self.shutterEvents -= 1
                    self.shutterLocal.tmpPath = tmpUrl!.path
                    self.shutterLocal.zoom = self.zoomSlider.value
                    self.shutterLocal.orientation = self.cameraPreview.getOrientation()
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
            
                self.cameraPreview.fireShutter(delegate: { [unowned self] (photo: AVCapturePhoto) -> Void
                in
                    let data = photo.fileDataRepresentation()
                    guard ( data != nil ) else { self.shutterReset();  return }
                    
                    let tmpUrl = self.saveToTmp(data: data!)
                    guard (tmpUrl != nil) else { self.shutterReset(); return }
                    
                    self.shutterLock.lock()
                        if (self.shutterEvents > 0)
                        {
                            self.shutterEvents -= 1
                            self.shutterLocal.tmpPath = tmpUrl!.path
                            self.shutterLocal.zoom = self.zoomSlider.value
                            self.shutterLocal.orientation = self.cameraPreview.getOrientation()
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
                    self.shutterRemote.tmpPath = tmpUrl!.path
                    self.shutterRemote.zoom = dataCmd.zoom
                    self.shutterRemote.orientation = dataCmd.orientation
                    self.shutterLock.signal()
                }
            self.shutterLock.unlock()
        }, timeout: 30.0)
    }
    
    func shutterReset()
    {
        self.shutterLock.lock()
            self.shutterEvents = -1
            self.shutterLocal = ShutterStruct()
            self.shutterRemote = ShutterStruct()
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
                self.shutterLocal = ShutterStruct()
                self.shutterRemote = ShutterStruct()
            }
        self.shutterLock.unlock()
        
        if (doReturn)
            { return }
    
        setLoaderMessage("Combining pictures ...")
        let localPtr = Bytes.toPointer(self.shutterLocal.tmpPath)
        let remotePtr = Bytes.toPointer(self.shutterRemote.tmpPath)
        
        let localSideVal = Cookie.instance.side
        let remoteSideVal = (localSideVal == LEFT) ? RIGHT : LEFT
        
        imageProcessor_setProcessorType(Int32(SPLIT.rawValue))
        imageProcessor_setImageN(Int32(localSideVal.rawValue), localPtr, Int32(CameraPreview.orientationToByte(self.shutterLocal.orientation)), self.shutterLocal.zoom)
        imageProcessor_setImageN(Int32(remoteSideVal.rawValue), remotePtr, Int32(CameraPreview.orientationToByte(self.shutterRemote.orientation)), self.shutterRemote.zoom)
        
        let outurl = Files.getRandomFile()
        guard (outurl != nil) else { shutterReset(); return }
        
        let outpath = outurl!.path
        let outptr = Bytes.toPointer(outpath)
        
        var swap:Int32 = 0
        if (cameraPreview.currentCamera?.position == AVCaptureDevice.Position.front)
            { swap = 1 }
        
        let growToMaxDim:Int32 = 0
        
        imageProcessor_preProcessN(swap)
        imageProcessor_processN(growToMaxDim, outptr )
        
        imageProcessor_cleanUpN()
        
        saveToPhotos(dataPath: outpath, onSaved:
        {(_path:String?) in
            DispatchQueue.main.async {
            [ unowned self ] in
            self.galleryBtn.update()
            }
        })
        sendProcessedPhoto(dataPath: outpath)
        
        self.shutterLock.lock()
            self.shutterEvents = 0
            self.shutterLocal = ShutterStruct()
            self.shutterRemote = ShutterStruct()
        self.shutterLock.unlock()
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
    
    @IBAction func openFaq(_ sender: Any)
    {
        performSegue(withIdentifier: "MasterCameraFAQSegue", sender: self)
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
    
    var safeAreaObj: AnyObject? = nil
    
    @IBOutlet weak var rightGalleryLeft: NSLayoutConstraint!
    @IBOutlet weak var rightControlsLeft: NSLayoutConstraint!
    @IBOutlet weak var rightPreviewLeft: NSLayoutConstraint!
    @IBOutlet weak var rightPreviewRight: NSLayoutConstraint!
    @IBOutlet weak var galleryRight: NSLayoutConstraint!
    @IBOutlet weak var previewLeft: NSLayoutConstraint!
    @IBOutlet weak var controlsLeft: NSLayoutConstraint!
    @IBOutlet weak var controlsRight: NSLayoutConstraint!
    
    private func updateTriggerLayout(size: CGSize? = nil)
    {
        let side = Cookie.instance.side
        let w:CGFloat
        let h:CGFloat
        if (size == nil)
        {
            w = view.frame.size.width - view.safeAreaInsets.left - view.safeAreaInsets.right
            h = view.frame.size.height
        }
        else
        {
            w = size!.width - view.safeAreaInsets.left - view.safeAreaInsets.right
            h = size!.height
        }
        
        if (side == LEFT)
        {
            if (w < h)
            {
                shutterLeft.constant = 100
                handBtnLeft.constant = w - 30 - handPhoneBtn.frame.width
            }
            else
            {
                shutterLeft.constant = 20
                handBtnLeft.constant = w - 40 - handPhoneBtn.frame.width
                
                galleryRight.isActive = true
                previewLeft.isActive = true
                controlsLeft.isActive = true
                controlsRight.isActive = true
                rightGalleryLeft.isActive = false
                rightControlsLeft.isActive = false
                rightPreviewLeft.isActive = false
                rightPreviewRight.isActive = false
            }
            
            handPhoneBtn.setImage(UIImage(named: "hand_phone"), for: .normal)
        }
        else
        {
            if (w < h)
            {
                shutterLeft.constant = w - 100 - shutterBtn.frame.width
                handBtnLeft.constant = 30
            }
            else
            {
                handBtnLeft.constant = 40
                shutterLeft.constant = w - 20 - handPhoneBtn.frame.width
                
                galleryRight.isActive = false
                previewLeft.isActive = false
                controlsLeft.isActive = false
                controlsRight.isActive = false
                rightGalleryLeft.isActive = true
                rightControlsLeft.isActive = true
                rightPreviewLeft.isActive = true
                rightPreviewRight.isActive = true
            }
            
            handPhoneBtn.setImage(UIImage(named: "hand_phone_right"), for: .normal)
        }
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator)
    {
        cameraPreviewOverlay.setNeedsDisplay()
        updateTriggerLayout(size: size)
        cameraPreview.setDrawPreviews(false)
        
        coordinator.animate(alongsideTransition: nil, completion:
        {
            [unowned self] (context: UIViewControllerTransitionCoordinatorContext)  in
            self.cameraPreview.setDrawPreviews(true)
        })
    }
    
    @IBOutlet weak var handBtnLeft: NSLayoutConstraint!
    @IBOutlet weak var shutterLeft: NSLayoutConstraint!
    
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

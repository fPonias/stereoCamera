/*
See LICENSE folder for this sample’s licensing information.

Abstract:
Implements the view controller for the camera interface.
*/

import UIKit
import AVFoundation
import Photos
import MetalKit
import CoreMedia
import CoreMotion


protocol DualCameraController
{
    func getZoom() -> Float
    func setZoom(_ zoom:Float)
    func configureSession() -> Bool
    func setCameraPair(pair: DualCameraCtrl.CameraPair)
    func viewWillAppear()
    func getSyncedFrames(callback:@escaping (_ left:CVPixelBuffer, _ right:CVPixelBuffer) -> Void)
}

class DualCameraCtrl: UIViewController
{
    @IBOutlet weak var leftHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var leftWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var rightHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var rightWidthConstraint: NSLayoutConstraint!
    @IBOutlet weak var leftOffsetConstraint: NSLayoutConstraint!
    
    @IBOutlet weak var cameraContainer: UIView!
    @IBOutlet weak var leftCameraPreview: VideoPreview!
    @IBOutlet weak var rightCameraPreview: VideoPreview!
    @IBOutlet weak var shutterBtn: UIButton!
    @IBOutlet weak var measureBtn: UIBarButtonItem!
    @IBOutlet weak var cameraSwapBtn: UIButton!
    
    private let zoomFinder = ZoomFinder()
    
    struct CameraPair{
        let left:AVCaptureDevice
        let right:AVCaptureDevice
    }
    
    var cameraPairs:[CameraPair] = Array()
    
    private let maxZoom = 3.0
    let sessionQueue = DispatchQueue(label: "session queue") // Communicate with the session and other session objects on this queue.
    var cameraCtrl:DualCameraController?
    
    
    // MARK: View Controller Life Cycle
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        let frm = cameraContainer.frame
        
        if (frm.height > frm.width / 2.0) {
            let w = frm.width / 2.0
            
            leftWidthConstraint.constant = w
            rightWidthConstraint.constant = w
            leftHeightConstraint.constant = w
            rightHeightConstraint.constant = w
            leftOffsetConstraint.constant = 0
        } else {
            let w = frm.height
            let margin = (frm.width - 2.0 * w) / 2.0
            
            leftWidthConstraint.constant = w
            rightWidthConstraint.constant = w
            leftHeightConstraint.constant = w
            rightHeightConstraint.constant = w
            leftOffsetConstraint.constant = margin
        }
        
        leftCameraPreview.resumeDrawing()
        rightCameraPreview.resumeDrawing()
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransition(to: size, with: coordinator)
        
        leftCameraPreview.stopDrawing()
        rightCameraPreview.stopDrawing()
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
                    
        leftCameraPreview?.initialize()
        rightCameraPreview?.initialize()
        motion.startAccelerometerUpdates()
        
        
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
        
        let sess:AVCaptureDevice.DiscoverySession
        if #available(iOS 13.0, *) {
            sess = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInTelephotoCamera, .builtInWideAngleCamera, .builtInUltraWideCamera, .builtInDualCamera, .builtInDualWideCamera, .builtInTripleCamera, .builtInTrueDepthCamera], mediaType: .video, position: .back)
            
            let pairs = sess.supportedMultiCamDeviceSets
            for pair in pairs {
                if (pair.count == 2) {
                    let pist = pair.sorted(by: { _,_ in return true })
                    if pist[0].formats[0].videoFieldOfView < pist[1].formats[0].videoFieldOfView {
                        cameraPairs.append(CameraPair(left: pist[0], right: pist[1]))
                    } else {
                        cameraPairs.append(CameraPair(left: pist[1], right: pist[0]))
                    }
                }
            }
        } else {
            sess = AVCaptureDevice.DiscoverySession(deviceTypes: [.builtInTelephotoCamera, .builtInWideAngleCamera], mediaType: .video, position: .back)
            
            
            let list = sess.devices
            //back tele, back ; back tele, back ultra ; back ultra, back ;
            //top - back, tele
            //bottom - tele
            //side - ultrawide, tele
        }
        
        guard (cameraPairs.count > 1)  else {
            print ("phone needs multiple cameras to function")
            return
        }
        
        /*
        Configure the capture session.
        In general it is not safe to mutate an AVCaptureSession or any of its
        inputs, outputs, or connections from multiple threads at the same time.
        
        Don't do this on the main queue, because AVCaptureMultiCamSession.startRunning()
        is a blocking call, which can take a long time. Dispatch session setup
        to the sessionQueue so as not to block the main queue, which keeps the UI responsive.
        */
        sessionQueue.async {
            if #available(iOS 13, *) {
                self.cameraCtrl = DualCameraMultiCameraCtrl(dualCameraCtrl: self)
                let result = self.cameraCtrl?.configureSession() ?? false
                
                if !result {
                    self.cameraCtrl = DualCameraLegacyCameraCtrl(dualCameraCtrl: self)
                    let result2 = self.cameraCtrl?.configureSession() ?? false
                    
                    if !result2 {
                        self.cameraCtrl = nil
                    }
                }
            } else {
                self.cameraCtrl = nil
            }
            
            if (self.cameraCtrl == nil) {
                return
            }
            
            self.setCameraPair(0)
            
            if self.viewWillAppearFlag {
                self.cameraCtrl?.viewWillAppear()
            }
        }
        
        // Keep the screen awake
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    @IBAction func swapCameraPairTapped(_ sender: Any) {
        let nextIdx = (currentCameraPair + 1) % cameraPairs.count
        setCameraPair(nextIdx)
    }
    
    private var currentCameraPair:Int = -1
    
    private func setCameraPair(_ index:Int) {
        guard (index >= 0 && index < cameraPairs.count) else { return }
        guard (index != currentCameraPair) else { return }
        
        currentCameraPair = index
        
        let pair = cameraPairs[index]
        print ("switching to left: \(pair.left.localizedName) and right: \(pair.right.localizedName)")
        
        cameraCtrl?.setCameraPair(pair: pair)
    }
    
    private var viewWillAppearFlag:Bool = false
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        viewWillAppearFlag = true
        
        if (cameraCtrl != nil) {
            cameraCtrl?.viewWillAppear()
        }
    }
    
    private func cleanUp()
    {
        motion.stopAccelerometerUpdates()
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        cleanUp()
        super.viewWillDisappear(animated)
    }
    
    @objc // Expose to Objective-C for use with #selector()
    private func didEnterBackground(notification: NSNotification) {
        cleanUp()
    }
    
    @objc // Expose to Objective-C for use with #selector()
    func willEnterForground(notification: NSNotification) {

    }
    
    // MARK: KVO and Notifications
    
    private func addObservers() {
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(didEnterBackground),
                                               name: NSNotification.Name.UIApplicationDidEnterBackground,
                                               object: nil)
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(willEnterForground),
                                               name: NSNotification.Name.UIApplicationWillEnterForeground,
                                               object: nil)
    }
    
    
    // MARK: Capture Session Management
    
    enum SessionSetupResult {
        case success
        case notAuthorized
        case configurationFailed
        case multiCamNotSupported
    }
    
    private var zoomLeft:CMSampleBuffer?
    private var zoomRight:CMSampleBuffer?
    private var zoomCalculated = true
    private var frameCounter = Date()
    private let motion = CMMotionManager()
    private var motionArr:[Double] = Array(repeating: 0.0, count: 5)
    
    private func calculateAngle() -> Double {
        var angle:Double
        
        if motion.isAccelerometerAvailable,
           let accel = motion.accelerometerData
        {
            let x = accel.acceleration.x
            let y = accel.acceleration.y
            
            if (abs(x) < 0.15) {
                if (y < 0) {
                    angle = 0.0
                } else {
                    angle = Double.pi
                }
            } else {
                angle = atan2(x, y)
                
                if (x < 0) {
                    angle += 2.0 * Double.pi
                }
                
                angle -= Double.pi
            }
            
            //print ("accel angle \(angle)")
        } else {
            angle = 0.0
        }
        
        //angle += Double.pi / 2.0
        
        motionArr.removeFirst()
        motionArr.append(angle)
        
        var total = 0.0
        for item in motionArr {
            total += item
        }
        
        return total / Double(motionArr.count)
    }
    
    func captureOutput(didOutput sampleBuffer: CMSampleBuffer, isLeft:Bool) {
        let angle = calculateAngle()
        
        if isLeft {
            leftCameraPreview.rotation = Float(angle)
            leftCameraPreview.renderBuffer(sampleBuffer: sampleBuffer)
        }
        else {
            rightCameraPreview.rotation = Float(angle)
            rightCameraPreview.renderBuffer(sampleBuffer: sampleBuffer)
        }
    }
    
    @IBAction func shutterClicked(_ sender: Any) {
        self.cameraCtrl?.getSyncedFrames(callback: {[weak self] (left, right) in
            
            self?.shutterClicked2(left, right)
        })
        
        showLoader(true)
    }
    
    private var loaderCtrl:LoadingPopupCtrl?
    private var loaderMessage:String = "Saving ..."
    
    @IBAction func measureClicked(_ sender: Any) {
        self.cameraCtrl?.getSyncedFrames(callback: {[weak self] (left, right) in
            
            self?.measureClicked2(left, right)
        })
        
        //showLoader(true)
    }
    
    func showLoader(_ show:Bool, message:String? = nil)
    {
        DispatchQueue.main.async
        {
            [unowned self] in
            if (message != nil)
                { loaderMessage = message! }
            
            if (show && loaderCtrl == nil)
            {
                loaderCtrl = LoadingPopupCtrl.initFromStoryboard()
                
                loaderCtrl!.header = loaderMessage
                
                present(loaderCtrl!, animated: true, completion: nil)
            }
            else if (!show && loaderCtrl != nil)
            {
                loaderCtrl = nil
                dismiss(animated: false, completion: nil)
            }
        }
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        if (segue.identifier == "measureSegue"){
            let vc = segue.destination as! MeasureCtrl
            let arr = sender as! [CVPixelBuffer]
            vc.leftPixels = arr[0]
            vc.rightPixels = arr[1]
        }
    }
    
    private func measureClicked2(_ lPixelBuffer:CVPixelBuffer, _ rPixelBuffer:CVPixelBuffer)
    {
        //self.showLoader(false)
        //sleep(100)
        
        DispatchQueue.main.async {
            self.performSegue(withIdentifier: "measureSegue", sender: [lPixelBuffer, rPixelBuffer])
        }
        
    }
    
    private let saver = Files.instance
    
    private func shutterClicked2(_ lPixelBuffer:CVPixelBuffer, _ rPixelBuffer:CVPixelBuffer)
    {
        let zoom:Float = 1.0
        let leftData = ImageEditorData(origData: lPixelBuffer, zoom: zoom, orientation: .DEG_0)
        let rightData = ImageEditorData(origData: rPixelBuffer, zoom: zoom, orientation: .DEG_0)
        
        let exporter = ImageExporter(leftData: leftData, rightData: rightData)
        exporter.export()
        
        showLoader(false)
    }
    
    private var current = -1
    private func nextTexture(proc: ImageProcessor) -> MTLTexture {
        current = (current + 1) % 3;
        switch(current){
        case 0: return proc._inTexture!
        case 1: return proc._midTexture!
        default: return proc._outTexture!
        }
    }
    
    public func alert(title: String, message: String, actions: [UIAlertAction]) {
        let alertController = UIAlertController(title: title,
                                                message: message,
                                                preferredStyle: .alert)
        
        actions.forEach {
            alertController.addAction($0)
        }
        
        self.present(alertController, animated: true, completion: nil)
    }
}

//
//  Cookie.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/1/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import AVKit

class Cookie
{
    let version:Float = 1.0

    let versionKey = "VERSION"
    let masterKey = "MASTER"
    let clientKey = "CLIENT"
    let cameraKey = "CAMERA"
    let cameraZoomsKey = "CAMERA_ZOOMS"
    let sideKey = "SIDE"
    
    static let _instance:Cookie = Cookie()
    
    static var instance:Cookie
    {
        get { return _instance }
    }
    
    init()
    {
        let ver = UserDefaults.standard.float(forKey: versionKey)
        if (ver < version)
        {
            updatePrefs()
        }
    }
    
    private func updatePrefs()
    {
        let ver = UserDefaults.standard.float(forKey: versionKey)
        let prefs = UserDefaults.standard
        
        if (ver == 0)
        {
            prefs.set(version, forKey: versionKey)
            prefs.set(false, forKey: masterKey)
            prefs.set("", forKey: clientKey)
            prefs.set(false, forKey: cameraKey)
            prefs.set([String:Float](), forKey: cameraZoomsKey)
            prefs.set(RIGHT.rawValue, forKey: sideKey)
        }
    }
    
    var master:Bool
    {
        get { return UserDefaults.standard.bool(forKey: masterKey) }
        set { UserDefaults.standard.set(newValue, forKey: masterKey) }
    }
    
    var client:String
    {
        get { return UserDefaults.standard.string(forKey: clientKey)! }
        set { UserDefaults.standard.set(newValue, forKey: clientKey) }
    }
    
    var camera: AVCaptureDevice.Position
    {
        get
        {
            let cameraInt = UserDefaults.standard.integer(forKey: cameraKey)
            return AVCaptureDevice.Position(rawValue: cameraInt)!
        }
        set
        {
            let value = newValue.rawValue
            UserDefaults.standard.set(value, forKey: cameraKey)
        }
    }
    
    func getZoomForClient(isMaster: Bool, client: String, camera: AVCaptureDevice.Position = AVCaptureDevice.Position.back) -> Float
    {
        let dict = UserDefaults.standard.dictionary(forKey: cameraZoomsKey)
        let key = (isMaster ? "1" : "0") + "-" + client + "-" + String(camera.rawValue)
        if (dict![key] != nil)
        {
            return dict![key] as! Float
        }
        else
        {
            return 1.0
        }
    }
    
    func setZoomForClient(zoom: Float, isMaster: Bool, client: String, camera: AVCaptureDevice.Position = AVCaptureDevice.Position.back)
    {
        var dict = UserDefaults.standard.dictionary(forKey: cameraZoomsKey)
        let key = (isMaster ? "1" : "0") + "-" + client + "-" + String(camera.rawValue)
        dict![key] = zoom
        
        UserDefaults.standard.set(dict, forKey: cameraZoomsKey)
    }
    
    var side:Side
    {
        get
        {
            let val = UserDefaults.standard.integer(forKey: sideKey)
            return Side(UInt32(val))
        }
        set { UserDefaults.standard.set(newValue.rawValue, forKey: sideKey) }
    }
}

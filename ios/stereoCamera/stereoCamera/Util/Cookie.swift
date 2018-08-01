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
    let overlayKey = "OVERLAY"
    let syncTestKey = "SYNC_TEST"
    let imageQualityKey = "IMAGE_QUALITY"
    
    enum PrefType
    {
        case OVERLAY,
        IMAGE_QUALITY
    }
    
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
    
    var overlay:Overlay
    {
        get
        {
            let val = UserDefaults.standard.integer(forKey: overlayKey)
            return Overlay(rawValue: val)!
        }
        set { UserDefaults.standard.set(newValue.rawValue, forKey: overlayKey)}
    }
    
    var imageQuality:ImageQuality
    {
        get
        {
            let val = UserDefaults.standard.integer(forKey: imageQualityKey)
            return ImageQuality(rawValue: val)!
        }
        set { UserDefaults.standard.set(newValue.rawValue, forKey: imageQualityKey)}
    }
    
    var runSyncTest:Bool
    {
        get { return UserDefaults.standard.bool(forKey: syncTestKey )}
        set { UserDefaults.standard.set(newValue, forKey: syncTestKey)}
    }
}



enum Overlay: Int
{
    case NONE,
    HALF,
    THIRDS,
    FOURTHS
    
    func toString() -> String
    {
        switch self
        {
            case .NONE: return "None"
            case .HALF: return "Half"
            case .THIRDS: return "Thirds"
            case .FOURTHS: return "Fourths"
        }
    }
    
    //this shouldn't be necessary, but swift lacks good reflection utilities
    static let allValues = [
        NONE.rawValue: NONE.toString(),
        HALF.rawValue: HALF.toString(),
        THIRDS.rawValue: THIRDS.toString(),
        FOURTHS.rawValue: FOURTHS.toString()
    ]
}

enum ImageQuality: Int
{
    case HIGH,
    LOW
    
    func toString() -> String
    {
        switch self
        {
            case .HIGH: return "High"
            case .LOW: return "Low"
        }
    }
    
    //this shouldn't be necessary, but swift lacks good reflection utilities
    static let allValues = [
        HIGH.rawValue: HIGH.toString(),
        LOW.rawValue: LOW.toString()
    ]
}

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
    let version:Float = 7.0

    let versionKey = "VERSION"
    let photoImageQualityKey = "PHOTO_IMAGE_QUALITY"
    let photoFormatKey = "PHOTO_FORMAT"
    let videoImageQualityKey = "VIDEO_IMAGE_QUALITY"
    let videoFormatKey = "VIDEO_FORMAT"
    let preferredOrientationKey = "PREFERRED_ORIENTATION"
    let introSeenKey = "INTRO_SEEN"
    
    enum PrefType
    {
        case PHOTO_IMAGE_QUALITY,
             PHOTO_IMAGE_FORMAT,
             VIDEO_IMAGE_QUALITY,
             VIDEO_IMAGE_FORMAT,
             ORIENTATION
    }
    
    private static let _instance:Cookie = Cookie()
    static var instance:Cookie
    {
        get { return _instance }
    }
    
    private init()
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
        
        if (ver < 6.0)
        {
            prefs.set(version, forKey: versionKey)
            prefs.set(false, forKey: introSeenKey)
        }
    }
    
    var introSeen:Bool
    {
        get { return UserDefaults.standard.bool(forKey: introSeenKey) }
        set { UserDefaults.standard.set(newValue, forKey: introSeenKey) }
    }
    
    var photoImageQuality:ImageQuality
    {
        get {
            let val = UserDefaults.standard.integer(forKey: photoImageQualityKey)
            return ImageQuality.init(rawValue: val) ?? ImageQuality.ULTRA_HI_DEF
        }
        set {
            UserDefaults.standard.setValue(newValue.rawValue, forKey: photoImageQualityKey)
        }
    }
    
    var photoFormat:Set<ImageFormat>
    {
        get {
            let val = UserDefaults.standard.integer(forKey: photoFormatKey)
            var ret = ImageFormat.intToSet(val)
            
            if ret.isEmpty {
                ret.insert(.SPLIT)
            }
            
            return ret
        }
        set {
            let val = ImageFormat.setToInt(newValue)
            UserDefaults.standard.setValue(val, forKey: photoFormatKey)
        }
    }
    
    var videoImageQuality:ImageQuality
    {
        get {
            let val = UserDefaults.standard.integer(forKey: videoImageQualityKey)
            return ImageQuality.init(rawValue: val) ?? ImageQuality.ULTRA_HI_DEF
        }
        set {
            UserDefaults.standard.setValue(newValue.rawValue, forKey: videoImageQualityKey)
        }
    }
    
    var videoFormat:ImageFormat {
        get {
            let val = UserDefaults.standard.integer(forKey: videoFormatKey)
            return ImageFormat.init(rawValue: val) ?? ImageFormat.SPLIT
        }
        set {
            UserDefaults.standard.setValue(newValue.rawValue, forKey: videoFormatKey)
        }
    }
    
    var preferredOrientation:UIDeviceOrientation {
        get {
            let exists = UserDefaults.standard.value(forKey: preferredOrientationKey)
            
            if (exists != nil) {
                let val = UserDefaults.standard.integer(forKey: preferredOrientationKey)
                return UIDeviceOrientation.init(rawValue: val) ?? UIDeviceOrientation.landscapeRight
            } else {
                return UIDeviceOrientation.landscapeRight
            }
        }
        set {
            UserDefaults.standard.setValue(newValue.rawValue, forKey: preferredOrientationKey)
        }
    }
}

enum ImageFormat: Int, CaseIterable
{
    case SPLIT = 0x1,
         GREEN_MAGENTA = 0x2,
         RED_BLUE = 0x4,
         ANIMATED = 0x8
    
    static func intToSet(_ val:Int) -> Set<ImageFormat> {
        var ret = Set<ImageFormat>()
        for value in ImageFormat.allCases {
            if (val & value.rawValue) != 0 {
                ret.insert(value)
            }
        }
        
        return ret
    }
    
    static func setToInt(_ newValue:Set<ImageFormat>) -> Int {
        var val = 0
        for value in ImageFormat.allCases {
            if newValue.contains(value) {
                val = val | value.rawValue
            }
        }
        
        return val
    }
    
    func toString() -> String {
        switch(self) {
        case .ANIMATED: return "animated"
        case .GREEN_MAGENTA: return "green-magenta"
        case .RED_BLUE: return "red-blue"
        case .SPLIT: return "split"
        }
    }
}

enum ImageQuality: Int, CaseIterable
{
    case STANDARD_DEF,
         HI_DEF,
         ULTRA_HI_DEF
    
    func toString() -> String {
        switch(self) {
        case .HI_DEF: return "HD"
        case .STANDARD_DEF: return "SD"
        case .ULTRA_HI_DEF: return "UHD"
        }
    }
}

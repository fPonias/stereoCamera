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
    let version:Float = 8.0

    let versionKey = "VERSION"
    let photoImageQualityKey = "PHOTO_IMAGE_QUALITY"
    let photoFormatKey = "PHOTO_FORMAT"
    let videoImageQualityKey = "VIDEO_IMAGE_QUALITY"
    let videoFormatKey = "VIDEO_FORMAT"
    let preferredOrientationKey = "PREFERRED_ORIENTATION"
    let introSeenKey = "INTRO_SEEN"
    let verticalOffsetKey = "VERTICAL_OFFSET"
    let zoomKey = "ZOOM"
    
    enum PrefType
    {
        case PHOTO_IMAGE_QUALITY,
             PHOTO_IMAGE_FORMAT,
             VIDEO_IMAGE_QUALITY,
             VIDEO_IMAGE_FORMAT,
             ORIENTATION,
             VERTICAL_OFFSET,
             ZOOM
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
        
        if (ver < 8.0)
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
    
    var videoImageQuality:VideoQuality
    {
        get {
            let val = UserDefaults.standard.integer(forKey: videoImageQualityKey)
            return VideoQuality.init(rawValue: val) ?? VideoQuality.MAX_H264
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
    
    var verticalOffset:CGFloat {
        get {
            let exists = UserDefaults.standard.value(forKey: verticalOffsetKey)
            
            if exists != nil {
                let val = UserDefaults.standard.float(forKey: verticalOffsetKey)
                return CGFloat(val)
            } else {
                return CGFloat(26)
            }
        }
        set {
            UserDefaults.standard.setValue(Float(newValue), forKey: verticalOffsetKey)
        }
    }
    
    var zoom:Float? {
        get {
            let exists = UserDefaults.standard.value(forKey: zoomKey)
            
            if exists != nil {
                let val = UserDefaults.standard.float(forKey: zoomKey)
                return val
            } else {
                return nil
            }
        }
        set {
            UserDefaults.standard.setValue(newValue, forKey: zoomKey)
        }
    }
}

enum ImageFormat: Int, CaseIterable
{
    case SPLIT = 0x1,
         GREEN_MAGENTA = 0x2,
         RED_BLUE = 0x4,
         ANIMATED = 0x8,
         SINGLE = 0x10
    
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
        case .SINGLE: return "single"
        }
    }
}

enum ImageQuality: Int, CaseIterable
{
    case STANDARD_DEF,
         HI_DEF,
         ULTRA_HI_DEF
    
    func toString() -> String {
        return "\(toInt()) pixels"
    }
    
    func toInt() -> Int {
        switch(self) {
        case .HI_DEF: return 480
        case .STANDARD_DEF: return 960
        case .ULTRA_HI_DEF: return 2160
        }
    }
}

enum VideoQuality: Int, CaseIterable
{
    case DVD,
         MAX_H264,
         BLU_RAY_H265
    
    func toString() -> String {
        return "\(toInt()) pixels"
    }
    
    func toInt() -> Int {
        switch(self) {
        case .DVD: return 480
        case .MAX_H264: return 960
        case .BLU_RAY_H265: return 1080
        }
    }
}

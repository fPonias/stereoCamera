//
//  Version.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/26/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Version : Command
{
    public enum Platform:UInt8
    {
        case IOS = 1,
        ANDROID = 2
    }

    var version:UInt32 = 0
    var platform:Platform = .IOS

    override init()
    {
        super.init()
        cmdtype = CommandTypes.SEND_VERSION
        expectsResponse = true
        
        version = Version.getVersion()
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result ) { return false }
        
        _ = comm.write(buf: [platform.rawValue])
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(version)
        
        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
        
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let (buf1, sz1) = comm.read(sz: 1)
        if (sz1 <= 0) {return false}
        
        let platformVal = buf1[0]
        if (platformVal <= 0 || platformVal > 2)
            { return false }
        platform = Platform.init(rawValue: buf1[0])!
        
        let (buf, sz) = comm.read(sz: 4)
        if (sz <= 0) { return false }
        
        version = Bytes.fromByteArray(buf)
        
        return true
    }
    
    static func getVersion() -> UInt32
    {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
        let parts = version.split(separator: ".")
        let sz = parts.count
        var ret:UInt32 = 0
        
        for i in 0 ... (sz - 1)
        {
            if (i == 0)
                { ret += UInt32(parts[i])! * 1000 }
            else if (i == 1)
                { ret += UInt32(parts[i])! * 100 }
            else
                { ret += UInt32(parts[i])! }
        }
        
        return ret
    }
}

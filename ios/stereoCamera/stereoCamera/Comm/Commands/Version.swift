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
    var version:Int32 = -1

    override init()
    {
        super.init()
        cmdtype = CommandTypes.SEND_VERSION
        expectsResponse = true
        
        version = Version.getVersion()
    }
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(version)
        
        let sz = comm.write(buf: bytes)
        
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let (buf, sz) = comm.read(sz: 4)
        version = Bytes.fromByteArray(buf)
    }
    
    static func getVersion() -> Int32
    {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String
        let parts = version.split(separator: ".")
        let sz = parts.count
        var ret:Int32 = 0
        
        for i in 0 ... (sz - 1)
        {
            if (i == 0)
                { ret += Int32(parts[i])! * 1000 }
            else if (i == 1)
                { ret += Int32(parts[i])! * 100 }
            else
                { ret += Int32(parts[i])! }
        }
        
        return ret
    }
}

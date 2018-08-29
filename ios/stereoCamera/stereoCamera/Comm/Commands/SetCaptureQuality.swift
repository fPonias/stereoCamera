//
//  SetCaptureQuality.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/1/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SetCaptureQuality : Command
{
    var quality:ImageQuality = ImageQuality.LOW
    
    init(_ quality:ImageQuality)
    {
        super.init()
        
        self.quality = quality
        doInit()
    }
    
    override init()
    {
        super.init()
        doInit()
    }
    
    func doInit()
    {
        cmdtype = CommandTypes.SET_CAPTURE_QUALITY
        expectsResponse = true
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(quality)

        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
       
        let bytes = [UInt8].init(repeating: 0, count: 1)
        let sz = comm.read(buffer: bytes)
        if (sz <= 0) { return false }
        
        quality = ImageQuality(rawValue: Int(bytes[0]))!
        return true
    }
}

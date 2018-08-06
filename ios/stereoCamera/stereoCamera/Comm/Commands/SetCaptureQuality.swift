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
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(quality)

        _ = comm.write(buf: bytes)
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let bytes = [UInt8].init(repeating: 0, count: 1)
        _ = comm.read(buffer: bytes)
        quality = ImageQuality(rawValue: Int(bytes[0]))!
    }
}

//
//  LatencyTest.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/17/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class LatencyTest : Command
{
    var elapsed:Int = 0
    
    override init()
    {
        super.init()
        cmdtype = CommandTypes.LATENCY_CHECK
        expectsResponse = true
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(elapsed)
        
        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let (buf2, sz2) = comm.read(sz:8)
        if (sz2 <= 0) { return false }
        
        elapsed = Bytes.fromByteArray(buf2)
        
        return true
    }
}

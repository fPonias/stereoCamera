//
//  SetCamera.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/27/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SetFacing : Command
{
    var facing:Bool = false

    override init()
    {
        super.init()
        doInit()
    }
    
    init(_ facing:Bool)
    {
        super.init()
        self.facing = facing
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.SET_FACING
        expectsResponse = true
    }
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(facing)
        
        let sz = comm.write(buf: bytes)
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let (buf, sz) = comm.read(sz: 1)
        facing = Bytes.fromByteArray(buf)
    }
}

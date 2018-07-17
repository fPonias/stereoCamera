//
//  SendGravity.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/13/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SendGravity : Command
{
    var gravity = Gravity(x: 0, y: 0, z: 0)
    
    override init()
    {
        super.init()
        doInit()
    }
    
    init(gravity:Gravity)
    {
        super.init()
        
        self.gravity = gravity
        doInit()
    }
    
    func doInit()
    {
        self.cmdtype = CommandTypes.RECEIVE_GRAVITY
        self.expectsResponse = false
    }
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(gravity.x)
        bytes += Bytes.toByteArray(gravity.y)
        bytes += Bytes.toByteArray(gravity.z)
        
        _ = comm.write(buf: bytes)
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let bytes = [UInt8].init(repeating: 0, count: 4)
        
        _ = comm.read(buffer: bytes)
        gravity.x = Bytes.fromByteArray( bytes)
        _ = comm.read(buffer: bytes)
        gravity.y = Bytes.fromByteArray(bytes)
        _ = comm.read(buffer: bytes)
        gravity.z = Bytes.fromByteArray(bytes)
    }
}

struct Gravity
{
    var x:Float
    var y:Float
    var z:Float
}

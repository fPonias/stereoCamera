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
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(gravity.x)
        bytes += Bytes.toByteArray(gravity.y)
        bytes += Bytes.toByteArray(gravity.z)
        
        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let bytes = [UInt8].init(repeating: 0, count: 4)
        
        let sz1 = comm.read(buffer: bytes)
        if (sz1 <= 0) { return false }
        gravity.x = Bytes.fromByteArray( bytes)
        
        let sz2 = comm.read(buffer: bytes)
        if (sz2 <= 0) { return false }
        gravity.y = Bytes.fromByteArray(bytes)
        
        let sz3 = comm.read(buffer: bytes)
        if (sz3 <= 0) { return false }
        gravity.z = Bytes.fromByteArray(bytes)
        
        
        return true
    }
}

struct Gravity
{
    var x:Float
    var y:Float
    var z:Float
}

//
//  SetZoom.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/6/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SetZoom : Command
{
    var zoom:Float = 1.0

    override init()
    {
        super.init()
        doInit()
    }
    
    init(_ zoom:Float)
    {
        super.init()
        self.zoom = zoom
        doInit()
    }
    
    func doInit()
    {
        cmdtype = CommandTypes.SET_ZOOM
        expectsResponse = true
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(zoom)
        
        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let (buf, sz) = comm.read(sz: 4)
        if (sz <= 0) { return false }
        zoom = Bytes.fromByteArray(buf)
        
        return true
    }
}

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
    public var localZoom:Float
    public var remoteZoom:Float

    override init()
    {
        localZoom = 1.0
        remoteZoom = 1.0
        super.init()
        
        doInit()
    }
    
    init(localZoom:Float, remoteZoom:Float)
    {
        self.localZoom = localZoom
        self.remoteZoom = remoteZoom
        super.init()
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
        bytes += Bytes.toByteArray(localZoom)
        bytes += Bytes.toByteArray(remoteZoom)
        
        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        var (buf, sz) = comm.read(sz: 4)
        if (sz <= 0) { return false }
        localZoom = Bytes.fromByteArray(buf)
        
        (buf, sz) = comm.read(sz: 4)
        if (sz <= 0) { return false }
        remoteZoom = Bytes.fromByteArray(buf)
        
        return true
    }
}

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
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(zoom)
        
        let sz = comm.write(buf: bytes)
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm) 
        
        let (buf, sz) = comm.read(sz: 4)
        zoom = Bytes.fromByteArray(buf)
    }
}

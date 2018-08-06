//
//  FireShutter.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/27/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class FireShutter : Command
{
    var data = [UInt8]()
    var zoom:Float = 1.0

    override init()
    {
        super.init()
        doInit()
    }
    
    init(data:Data, zoom:Float)
    {
        super.init()
        
        self.data = [UInt8](data)
        self.zoom = zoom
        
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.FIRE_SHUTTER
        expectsResponse = true
    }
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(zoom)
        
        let sz = data.count
        bytes += Bytes.toByteArray(sz)
        let _ = comm.write(buf: bytes)
        
        if (sz > 0)
            { let _ = comm.write(buf: data) }
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let(buf2, sz2) = comm.read(sz:8)
        zoom = Bytes.fromByteArray(buf2)
        
        let (buf3, sz3) = comm.read(sz: 8)
        let bufSz:Int = Bytes.fromByteArray(buf3)
        
        data = [UInt8](repeating: 0, count: bufSz)
        
        if (bufSz == 0)
            { return }
        
        let _ = comm.read(buffer: data)
    }
}

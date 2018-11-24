//
//  FireShutter.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/27/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import GLKit

class FireShutter : Command
{
    var data = [UInt8]()
    var zoom:Float = 1.0
    var orientation:CameraPreview.CameraOriention = .DEG_0

    override init()
    {
        super.init()
        doInit()
    }
    
    init(data:Data, zoom:Float, orientation:CameraPreview.CameraOriention)
    {
        super.init()
        
        self.data = [UInt8](data)
        self.zoom = zoom
        self.orientation = orientation
        
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.FIRE_SHUTTER
        expectsResponse = true
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(zoom)
        
        print ("writing Float value " + String(zoom))
        
        let orientB = CameraPreview.orientationToByte(orientation)
        bytes += [orientB]
        
        let sz = data.count
        bytes += Bytes.toByteArray(sz)
        let sz1 = comm.write(buf: bytes)
        if (sz1 <= 0) { return false }
        
        print ("writing Int value " + String(sz))
        
        if (sz > 0)
        {
            let sz2 = comm.write(buf: data)
            if ( sz2 <= 0) { return false }
        }
        
        return true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let(buf2, sz2) = comm.read(sz:5)
        if (sz2 <= 0) { return false }
        zoom = Bytes.fromByteArray(buf2)
        
        print ("read zoom value " + String(zoom))
        
        var orientB = buf2[4]
        orientation = CameraPreview.orientationFromByte(orientB)
        
        print ("read orientation value " + String(orientB))
        
        let (buf3, sz3) = comm.read(sz: 8)
        if (sz3 <= 0) { return false }
        let bufSz:Int = Bytes.fromByteArray(buf3)
        
        print ("read size value " + String(bufSz))
        
        data = [UInt8](repeating: 0, count: bufSz)
        
        if (bufSz == 0)
            { return true }
        
        let sz4 = comm.read(buffer: data)
        return (sz4 <= 0) ? false : true
    }
}

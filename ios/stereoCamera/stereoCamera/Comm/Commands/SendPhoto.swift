//
//  FireShutter.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/27/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SendPhoto : Command
{
    var data = [UInt8]()

    override init()
    {
        super.init()
        doInit()
    }
    
    init(dta:Data)
    {
        super.init()
        data = [UInt8](dta)
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.SEND_PROCESSED_PHOTO
        expectsResponse = true
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        let sz = data.count
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(sz)
        let sz1 = comm.write(buf: bytes)
        if (sz1 <= 0) { return false }
        
        if (sz > 0)
        {
            let sz2 = comm.write(buf: data)
            if (sz2 <= 0) { return false }
        }
        
        return true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let (buf, sz) = comm.read(sz: 8)
        if (sz <= 0) { return false }
        
        let bufSz:Int = Bytes.fromByteArray(buf)
        if (bufSz <= 0) { return true}
        
        data = [UInt8](repeating: 0, count: bufSz)
        
        let sz2 = comm.read(buffer: data)
        return (sz2 <= 0) ? false : true
    }
}

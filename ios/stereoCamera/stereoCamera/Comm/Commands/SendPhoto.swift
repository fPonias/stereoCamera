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
        expectsResponse = false
    }
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        
        let sz = data.count
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(sz)
        let _ = comm.write(buf: bytes)
        
        if (sz > 0)
            { let _ = comm.write(buf: data) }
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let (buf, sz) = comm.read(sz: 8)
        let bufSz:Int = Bytes.fromByteArray(buf)
        
        data = [UInt8](repeating: 0, count: bufSz)
        
        if (bufSz == 0)
            { return }
        
        let _ = comm.read(buffer: data)
    }
}

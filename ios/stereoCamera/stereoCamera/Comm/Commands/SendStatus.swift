//
//  SendStatus.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/25/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SendStatus : Command
{
    var status:Status

    init(_ status: Status)
    {
        self.status = status
        super.init()
        doInit()
    }
    
    override init()
    {
        self.status = Status.NONE
        super.init()
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.RECEIVE_STATUS
        expectsResponse = false
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (!result) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(status)
        
        let sz = comm.write(buf: bytes)
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (!result) { return false }
        
        let (buf, sz) = comm.read(sz: 1)
        if (sz <= 0) { return false }
        status = Bytes.fromByteArray(buf)
        
        return true
    }
}

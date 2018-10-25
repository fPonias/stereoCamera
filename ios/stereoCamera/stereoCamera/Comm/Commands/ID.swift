//
//  ID.swift
//  stereoCamera
//
//  Created by hallmarklabs on 8/23/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class ID : Command
{
    public var phoneId:String = ""
    
    override init()
    {
        super.init()
        doInit()
    }
    
    init(phoneId:String)
    {
        super.init()
        self.phoneId = phoneId
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.ID
        expectsResponse = true
    }

    
    override func send(comm: Comm) -> Bool
    {
        let success = super.send(comm: comm)
        if (!success)
            { return false }
        
        
        let strBytes:[UInt8] = Array(phoneId.utf8)
        let sz = strBytes.count
        let szBytes:[UInt8] = Bytes.toByteArray(sz)
        
        
        let sz1 = comm.write(buf: szBytes)
        if (sz1 <= 0)
            { return false }
        
        let sz2 = comm.write(buf: strBytes)
        if (sz2 <= 0)
            { return false }
            
        return true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let success = super.receive(comm: comm)
        if (!success)
            { return false }
        
        let (buf, sz) = comm.read(sz:8)
        if (sz <= 0)
            { return false }
        let bufSz:Int = Bytes.fromByteArray(buf)
        
        let (buf2, sz2) = comm.read(sz:bufSz)
        if (sz2 <= 0)
            { return false }
        phoneId = String(bytes: buf2, encoding: .utf8)!
        
        return true
    }
}

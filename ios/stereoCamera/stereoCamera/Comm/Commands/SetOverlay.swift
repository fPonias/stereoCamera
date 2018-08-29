//
//  SetOverlay.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/27/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SetOverlay : Command
{
    var overlay = Overlay.NONE

    override init()
    {
        super.init()
        doInit()
    }
    
    init(_ overlay: Overlay)
    {
        super.init()
        self.overlay = overlay
        doInit()
    }
    
    private func doInit()
    {
        cmdtype = CommandTypes.SET_OVERLAY
        expectsResponse = true
    }
    
    override func send(comm: Comm) -> Bool
    {
        let result = super.send(comm: comm)
        if (result == false) { return false }
        
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(overlay)
        
        let sz = comm.write(buf: bytes)
        
        return (sz <= 0) ? false : true
    }
    
    override func receive(comm: Comm) -> Bool
    {
        let result = super.receive(comm: comm)
        if (result == false) { return false }
        
        let (buf, sz) = comm.read(sz: 1)
        if (sz <= 0) { return false }
        
        overlay = Bytes.fromByteArray(buf)
        return true
    }
}

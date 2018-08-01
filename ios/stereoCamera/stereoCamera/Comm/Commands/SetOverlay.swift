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
    
    override func send(comm: Comm)
    {
        super.send(comm: comm)
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(overlay)
        
        let sz = comm.write(buf: bytes)
    }
    
    override func receive(comm: Comm)
    {
        super.receive(comm: comm)
        
        let (buf, sz) = comm.read(sz: 1)
        overlay = Bytes.fromByteArray(buf)
    }
}

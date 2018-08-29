//
//  Command.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/15/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Command
{
    static var nextId:Int = 1
    
    public var cmdtype:CommandTypes = CommandTypes.NONE
    public var id:Int
    public var isResponse:Bool = false
    
    var expectsResponse:Bool = false
    
    init()
    {
        id = Command.nextId
        Command.nextId = Command.nextId + 1
    }
    
    func send(comm:Comm) -> Bool
    {    
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(cmdtype)
        bytes += Bytes.toByteArray(id)
        bytes += Bytes.toByteArray(isResponse)
        
        let sz = comm.write(buf: bytes)
        if (sz <= 0)
            { return false }
        
        return true
    }
    
    func receive(comm:Comm) -> Bool
    {
        let (buf, sz) = comm.read(sz: 9)
        
        if (sz <= 0)
            { return false }
        
        id = Bytes.fromByteArray(buf)
        isResponse = Bytes.fromByteArray(buf, 8)
        
        return true
    }
    
    
    
    func onResponse(command:Command)
    {}
}

class DefaultCommand : Command
{
    override init()
    {
        super.init()
        self.cmdtype = CommandTypes.NONE
    }
}

class CommandFactory
{
    static func build(type: CommandTypes) -> Command
    {
        switch type
        {
        case CommandTypes.PING:
            return Ping()
        case CommandTypes.HANDSHAKE:
            return Handshake()
        case CommandTypes.SEND_VERSION:
            return Version()
        case CommandTypes.RECEIVE_STATUS:
            return SendStatus()
        case CommandTypes.SET_FACING:
            return SetFacing()
        case CommandTypes.SET_OVERLAY:
            return SetOverlay()
        case CommandTypes.SET_ZOOM:
            return SetZoom()
        case CommandTypes.RECEIVE_ZOOM:
            return SendZoom()
        case CommandTypes.FIRE_SHUTTER:
            return FireShutter()
        case CommandTypes.SEND_PROCESSED_PHOTO:
            return SendPhoto()
        case CommandTypes.DISCONNECT:
            return Disconnect()
        case CommandTypes.RECEIVE_DISCONNECT:
            return SendDisconnect()
        case CommandTypes.CONNECTION_PAUSE:
            return ConnectionPause()
        case CommandTypes.RECEIVE_CONNECTION_PAUSE:
            return SendConnectionPause()
        case CommandTypes.RECEIVE_GRAVITY:
            return SendGravity()
        case CommandTypes.SET_CAPTURE_QUALITY:
            return SetCaptureQuality()
        case CommandTypes.LATENCY_CHECK:
            return LatencyTest()
        case CommandTypes.ID:
            return ID()
        default:
            print("CommandFactory unable to construct command of type " + type.description)
            return DefaultCommand()
        }
    }
}

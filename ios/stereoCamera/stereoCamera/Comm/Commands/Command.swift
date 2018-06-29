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
    
    func send(comm:Comm)
    {
        var bytes:[UInt8] = []
        bytes += Bytes.toByteArray(cmdtype)
        bytes += Bytes.toByteArray(id)
        bytes += Bytes.toByteArray(isResponse)
        
        let sz = comm.write(buf: bytes)
        
        //TODO handle error
    }
    
    func receive(comm:Comm)
    {
        let (buf, sz) = comm.read(sz: 9)
        
        //TODO handle error
        
        id = Bytes.fromByteArray(buf)
        isResponse = Bytes.fromByteArray(buf, 8)
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
        case CommandTypes.FIRE_SHUTTER:
            return FireShutter()
        case CommandTypes.SEND_PROCESSED_PHOTO:
            return SendPhoto()
        default:
            print("CommandFactory unable to construct command of type " + type.description)
            return DefaultCommand()
        }
    }
}

//
//  Ping.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/15/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Ping : Command
{
    private let start:Date = Date.init()
    
    private var _value:Double = -1
    public var value:Double { get {return _value }}
    
    override init()
    {
        super.init()
        cmdtype = CommandTypes.PING
        expectsResponse = true
    }
    
    override func onResponse(command: Command)
    {
        let end:Date = Date.init();
        _value = end.timeIntervalSince(start)
        
        print ("ping took " + String(_value))
    }
}

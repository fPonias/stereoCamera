//
//  HandshakeTimeout.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/25/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Handshake : Command
{    
    override init()
    {
        super.init()
        cmdtype = CommandTypes.HANDSHAKE
        expectsResponse = true
    }
}

//
//  ConnectionPause.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/9/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class ConnectionPause : Command
{
    override init()
    {
        super.init()
        cmdtype = CommandTypes.CONNECTION_PAUSE
        expectsResponse = false
    }
}


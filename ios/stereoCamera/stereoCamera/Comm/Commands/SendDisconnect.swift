//
//  SendDisconnect.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/9/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SendDisconnect : Command
{
    override init()
    {
        super.init()
        cmdtype = CommandTypes.RECEIVE_DISCONNECT
        expectsResponse = false
    }
}

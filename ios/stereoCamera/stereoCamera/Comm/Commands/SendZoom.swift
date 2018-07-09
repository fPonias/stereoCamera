//
//  SendZoom.swift
//  stereoCamera
//
//  Created by hallmarklabs on 7/6/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class SendZoom : SetZoom
{
    override func doInit()
    {
        super.doInit()
        
        cmdtype = CommandTypes.RECEIVE_ZOOM
        expectsResponse = false
    }
}

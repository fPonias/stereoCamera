//
//  CommandTypes.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/15/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

enum CommandTypes: Int
{
    case NONE,
    FIRE_SHUTTER,
    PING,
    LATENCY_CHECK,
    RECEIVE_ANGLE_OF_VIEW,
    SET_ZOOM,
    SET_FACING,
    SET_OVERLAY,
    SEND_VERSION,
    RECEIVE_ZOOM,
    RECEIVE_STATUS,
    RECEIVE_GRAVITY,
    RECEIVE_ORIENTATION,
    CONNECTION_PAUSE,
    RECEIVE_CONNECTION_PAUSE,
    DISCONNECT,
    RECEIVE_DISCONNECT,
    SEND_PROCESSED_PHOTO,
    RECEIVE_PREVIEW_FRAME,
    HANDSHAKE,
    SET_CAPTURE_QUALITY
    
    var description: String
    {
        switch self{
        case .NONE: return "None"
        case .FIRE_SHUTTER: return "Fire shutter"
        case .PING: return "Ping"
        case .LATENCY_CHECK: return "Latency check"
        case .RECEIVE_ANGLE_OF_VIEW: return "Receive angle of view"
        case .SET_ZOOM: return "Set zoom"
        case .SET_FACING: return "Set facing"
        case .SET_OVERLAY: return "Set overlay"
        case .SEND_VERSION: return "Send version"
        case .RECEIVE_ZOOM: return "Receive zoom"
        case .RECEIVE_STATUS: return "Receive status"
        case .RECEIVE_GRAVITY: return "Receive gravity"
        case .RECEIVE_ORIENTATION: return "Receive orientation"
        case .CONNECTION_PAUSE: return "Connection pause"
        case .RECEIVE_CONNECTION_PAUSE: return "Receive connection pause"
        case .DISCONNECT: return "Disconnect"
        case .RECEIVE_DISCONNECT: return "Recieve disconnect"
        case .SEND_PROCESSED_PHOTO: return "Send processed photo"
        case .RECEIVE_PREVIEW_FRAME: return "Receive preview frame"
        case .HANDSHAKE: return "Handshake"
        case .SET_CAPTURE_QUALITY: return "set capture quality"
        default: return "Undefined"
        }
    }
}

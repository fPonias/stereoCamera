//
//  SlaveState.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/25/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class SlaveState : CommandListener
{
    var status:Status = .NONE
    var zoom:Float = 1.0
    var horizFov:Float = 100.0
    var vertFov:Float = 100.0
    var gravity:Any = 0
    var orientation:UIDeviceOrientation = .portrait
    
    var listener:SlaveStateListener? = nil
    
    func onCommand(_ command: Command)
    {
        switch(command.cmdtype)
        {
            case .RECEIVE_STATUS:
                self.lock.lock()
                    let sendStatus = command as! SendStatus
                    self.status = sendStatus.status
                    self.lock.signal()
                self.lock.unlock()
            case .RECEIVE_ANGLE_OF_VIEW:
                break
            case .RECEIVE_GRAVITY:
                break
            case .RECEIVE_ZOOM:
                break
            case .RECEIVE_ORIENTATION:
                break
            case .RECEIVE_CONNECTION_PAUSE:
                break
            case .RECEIVE_DISCONNECT:
                break
            case .RECEIVE_PREVIEW_FRAME:
                break
            default:
                break
        }
    }
    
    func start()
    {
        CommManager.instance.comm.commandListener = self
    }
    
    private let lock = NSCondition()
    
    func waitOnStatus(statusList:[Status], timeout:TimeInterval) -> Bool
    {
        if (isPresent(status: status, statusList: statusList))
            { return true }

        let then = Date()
        var diff:TimeInterval = 0

        repeat
        {
            lock.lock()
                if (!isPresent(status: status, statusList: statusList))
                {
                    let delay = timeout - diff
                    lock.wait(until: Date(timeIntervalSinceNow: TimeInterval(delay)))
                }
            lock.unlock()

            let now = Date()
            diff = now.timeIntervalSince(then);
        } while (diff < timeout && !isPresent(status: status, statusList: statusList))

        return (isPresent(status: status, statusList: statusList));
    }

    private func isPresent(status: Status, statusList: [Status]) -> Bool
    {
        for st in statusList
        {
            if (status == st)
                { return true }
        }

        return false;
    }
    
    func waitOnStatusAsync(status: Status, timeout: TimeInterval, listener: @escaping BoolListener)
    {
        waitOnStatusAsync(statusList: [status], timeout: timeout, listener: listener);
    }

    func waitOnStatusAsync(statusList: [Status], timeout: TimeInterval, listener: @escaping BoolListener)
    {
        DispatchQueue.global(qos: .utility).async
        {
            let success = self.waitOnStatus(statusList: statusList, timeout: timeout);
            listener(success)
        }
    }
}

protocol SlaveStateListener
{
    func onStatus(status:Status)
    func onZoom(zoom:Float)
    func onFov(horiz:Float, vert:Float)
    func onGravity(gravity:Any)
    func onOrientation(orientation:UIDeviceOrientation)
    func onConnectionPause()
    func onDisconnect()
    func onPreviewFrame(data:[UInt8], zoom:Float)
}

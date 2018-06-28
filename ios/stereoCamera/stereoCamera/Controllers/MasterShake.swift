//
//  MasterShake.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/24/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class MasterShake
{
    private var steps = [MasterShakeStep]()
    private let lock = NSCondition()
    private var handshaking = false;
    private var finalListener:MasterShakeListener?
    private var stepListener:MasterShakeListener?
    private var currentStep = -1
    public var parent:CameraMasterCtrl? = nil
    
    init()
    {
        steps.append(PingStep())
        steps.append(VersionCheckStep())
        steps.append(CreatedStep(parent: self))
        steps.append(HandshakeStep())
        steps.append(ReadyStep(parent: self))
        steps.append(SetCameraStep())
        steps.append(SetOverlayStep())
    }
    
    class PingStep : MasterShakeStep
    {
        var name:String
        {
            get { return "ping" }
        }
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = Ping()
            CommManager.instance.comm.sendCommand(command: cmd, listener: {(_ origCmd: Command?, _ respCmd: Command) -> Void
            in
                listener(true)
            })
        }
    }
    
    class VersionCheckStep : MasterShakeStep
    {
        var name: String { get { return "version check" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = Version()
            CommManager.instance.comm.sendCommand(command: cmd, listener: {(origCmd: Command?, respCmd: Command) -> Void
            in
                if ((origCmd as! Version).version == (respCmd as! Version).version)
                    { listener(true) }
                else
                    { listener(false) }
            })
        }
    }

    class HandshakeStep : MasterShakeStep
    {
        var name:String
        {
            get { return "handshake" }
        }
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = Handshake()
            CommManager.instance.comm.sendCommand(command: cmd, listener: {(_ origCmd: Command?, _ respCmd: Command) -> Void
            in
                listener(true)
            })
        }
    }

    class ReadyStep : MasterShakeStep
    {
        let parent:MasterShake
        init(parent: MasterShake)
        {
            self.parent = parent
        }
    
        var name:String
        {
            get { return "ready"}
        }
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let statusList = [Status.LISTENING, Status.READY]
            parent.parent?.slaveState.waitOnStatusAsync(statusList: statusList, timeout: 3.0, listener: {(success: Bool) -> Void
            in
                listener(success)
            })
        }
    }
    
    class CreatedStep : MasterShakeStep
    {
        let parent:MasterShake
        init(parent: MasterShake)
        {
            self.parent = parent
        }
    
        var name:String { get { return "created"} }
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let statusList = [Status.RESUMED, Status.CREATED]
            parent.parent?.slaveState.waitOnStatusAsync(statusList: statusList, timeout: 3.0, listener: {(success: Bool) -> Void
            in
                listener(success)
            })
        }
    }
    
    class SetCameraStep : MasterShakeStep
    {
        var name:String { get { return "set camera" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = SetFacing(false)
            CommManager.instance.comm.sendCommand(command: cmd, listener: {(_ origCmd: Command?, _ respCmd: Command) -> Void
            in
                listener(true)
            })
        }
    }
    
    class SetOverlayStep : MasterShakeStep
    {
        var name:String { get { return "set overlay" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = SetOverlay(CameraPreviewOverlayType.None)
            CommManager.instance.comm.sendCommand(command: cmd, listener: {(_ origCmd: Command?, _ respCmd: Command) -> Void
            in
                listener(true)
            })
        }
    }
    
    func start(listener: @escaping MasterShakeListener)
    {
        var doReturn = false
        lock.lock()
            if (handshaking)
                { doReturn = true }
            else
                { handshaking = true }
        lock.unlock()
        
        if (doReturn)
            { return }
        
        finalListener = listener
        stepListener = {(success:Bool) ->Void
        in
            if (success)
            {
                print ("step success")
                self.nextStep()
            }
            else
            {
                print ("step fail")
                self.runFail()
            }
        }
        
        currentStep = -1
        nextStep();
    }
    
    private func nextStep()
    {
        currentStep += 1
        
        if (currentStep == steps.count)
        {
            lock.lock()
                handshaking = false
            lock.unlock()
            
            print ("handshake success")
            finalListener?(true)
            return
        }
        
        let step = steps[currentStep]
        print("executing handshake step " + String(currentStep) + ": " + step.name)
        
        if (stepListener != nil)
            {step.execute(listener: stepListener!)}
        else
            {finalListener?(false)}
    }
    
    private func runFail()
    {
        lock.lock()
            handshaking = false
        lock.unlock()
        
        finalListener?(false)
    }
}

typealias MasterShakeListener = (_ success:Bool) -> Void

protocol MasterShakeStep
{
    var name:String {get}
    func execute(listener: @escaping MasterShakeListener)
}



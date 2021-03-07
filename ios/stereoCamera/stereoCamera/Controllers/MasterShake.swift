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
        steps.append(HandshakeStep())
        steps.append(VersionCheckStep())
        steps.append(IDStep())
        steps.append(SetCameraStep(parent: self))
        steps.append(SetQualityStep())
        steps.append(SetZoomStep(parent: self))
        steps.append(SetOverlayStep())
        steps.append(ReadyStep(parent: self))
    }
    
    class StepListener : CommandResponseListener
    {
        var received:(Command, Command?) -> Void
        var failed:() -> Void
        
        init(received: @escaping (Command, Command?) -> Void, failed: @escaping () -> Void)
        {
            self.received = received
            self.failed = failed
        }

        override func onReceived(_ command: Command, origCommand: Command?)
        {
            received(command, origCommand)
        }
        
        override func onReceiveFailed(_ command: Command)
        {
            failed()
        }
    }
    
    class DefaultStepListener : CommandResponseListener
    {
        var listener:MasterShakeListener
        
        init(_ listener:@escaping MasterShakeListener)
        {
            self.listener = listener
        }
        
        override func onReceived(_ command: Command, origCommand: Command?)
        {
            listener(true)
        }
        
        override func onReceiveFailed(_ command: Command)
        {
            listener(false)
        }
    }
    
    class PingStep : MasterShakeStep
    {
        var attempt:Int = 0
    
        var name:String
        {
            get { return "ping" }
        }
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = Ping()
            CommManager.instance.comm.sendCommand(command: cmd, listener: StepListener(
                received: { (command:Command, origCmd:Command?) in
                    listener(true)
                },
                failed: { [unowned self] in
                    self.attempt += 1
            
                    if (self.attempt == 3)
                        { listener (false) }
                    else
                    {
                        Thread.sleep(forTimeInterval: 1000)
                        self.execute(listener: listener)
                    }
                })
            )
        }
    }
    
    class VersionCheckStep : MasterShakeStep
    {
        var name: String { get { return "version check" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = Version()
            CommManager.instance.comm.sendCommand(command: cmd, listener: StepListener(
                received: {(_ respCmd: Command, _ origCmd: Command?) -> Void
                in
                    let orig = origCmd as! Version
                    let resp = respCmd as! Version
                    
                    Cookie.instance.setCurrentClientVersion(value: resp.version)
                    Cookie.instance.setCurrentClientPlatform(value: resp.platform)
                    
                    if (resp.platform == .IOS && orig.version == resp.version)
                        { listener(true) }
                    else if (resp.platform == .ANDROID && resp.version == 1200)
                        { listener(true) }
                    else
                        { listener(false) }
                },
                failed: {() -> Void
                in
                    listener(false)
                }
            ))
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
            CommManager.instance.comm.sendCommand(command: cmd, listener: DefaultStepListener(listener))
        }
    }
    
    class IDStep : MasterShakeStep
    {
        var name:String
        {
            get { return "id" }
        }
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let cmd = ID(phoneId: Cookie.instance.id)
            CommManager.instance.comm.sendCommand(command: cmd, listener: StepListener(
                received:
                { (_ respCmd: Command, _ origCmd: Command?) -> Void in
                    let cmd:ID = respCmd as! ID
                    Cookie.instance.setCurrentClientID(value: cmd.phoneId)
                    listener(true)
                },
                failed:
                {() -> Void in
                    listener(false)
                }
            ))
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
        unowned let parent:MasterShake
        init(parent: MasterShake)
        {
            self.parent = parent
        }
        var name:String { get { return "set camera" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let position = parent.parent?.cameraPreview.currentCamera?.position
            let posBool = (position != nil && position == .front) ? true : false
            
            let cmd = SetFacing(posBool)
            CommManager.instance.comm.sendCommand(command: cmd, listener: DefaultStepListener(listener))
        }
    }
    
    class SetZoomStep : MasterShakeStep
    {
        unowned let parent:MasterShake
        init(parent: MasterShake)
        {
            self.parent = parent
        }
        var name:String { get { return "set zoom" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let zoom = Cookie.instance.getZoomForClient(isMaster: false, client: CommManager.instance.comm.address, camera: (parent.parent?.cameraPreview.currentCamera?.position)!)
            let cmd = SetZoom(localZoom: 1.0, remoteZoom: zoom)
            CommManager.instance.comm.sendCommand(command: cmd, listener: DefaultStepListener(listener))
        }
    }
    
    class SetOverlayStep : MasterShakeStep
    {
        var name:String { get { return "set overlay" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let overlay = Cookie.instance.overlay
            let cmd = SetOverlay(overlay)
            CommManager.instance.comm.sendCommand(command: cmd, listener: DefaultStepListener(listener))
        }
    }
    
    class SetQualityStep : MasterShakeStep
    {
        var name: String { get { return "set image quality" }}
        
        func execute(listener: @escaping MasterShakeListener)
        {
            let quality = Cookie.instance.imageQuality
            let cmd = SetCaptureQuality(quality)
            CommManager.instance.comm.sendCommand(command: cmd, listener: DefaultStepListener(listener))
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



//
//  Comm.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/14/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Comm
{
    private var cppPtr:UnsafeRawPointer = commNew()
    
    deinit
    {
        commandReceiverIsRunning = false;
        
        if(commandSenderIsRunning)
        {
            commandSenderCondition.lock()
            commandSenderIsRunning = false
            commandSenderCondition.signal()
            commandSenderCondition.unlock()
        }
        
        commCleanUp(cppPtr)
    }
    
    func connect(onConnected connected: @escaping () -> Void, onFail fail: @escaping () -> Void)
    {
        DispatchQueue.global(qos: .userInitiated).async
        {
            if (CommManager.instance.isMaster)
                { commStartServer(self.cppPtr, CommManager.PORT) }
            else
                { commStartClient(self.cppPtr, CommManager.instance.masterAddresses[0], CommManager.PORT) }
            
            if (commIsConnected(self.cppPtr) > 0)
            {
                print("comm connected starting sender and receiver")
                self.startCommandSender()
                self.startCommandReceiver()
                
                usleep(250000)
                
                print("comm connected")
                connected()
            }
            else
                { fail() }
        }
    }
    
    func read(sz:Int) -> ([UInt8], Int32)
    {
        let buf = [UInt8](repeating: 0, count: sz)
        let bufPtr = UnsafeMutablePointer<UInt8>(mutating: buf)
        let szRead = commRead(cppPtr, bufPtr, Int32(sz))
        
        return (buf, szRead)
    }
    
    func write(buf:[UInt8]) -> Int32
    {
        let sz = buf.count
        let bufPtr = UnsafePointer<UInt8>(buf)
        let szWritten = commWrite(cppPtr, bufPtr, Int32(sz))
        
        return szWritten
    }
    
    private var commandReceiverIsRunning:Bool = false
    
    func startCommandReceiver()
    {
        if (self.commandReceiverIsRunning)
        {return}
        
        DispatchQueue.global(qos: .background).async
        {
            if (self.commandReceiverIsRunning)
                {return}
            
            print("starting command receiver")
            self.commandReceiverIsRunning = true
            let buffer = [UInt8].init(repeating: 0, count: 1)
            let bufferPtr = UnsafeMutablePointer<UInt8>(mutating: buffer)
            
            while(self.commandReceiverIsRunning)
            {
                let sz = commRead(self.cppPtr, bufferPtr, 1)
                
                if (sz != 1 || !self.commandReceiverIsRunning)
                {
                    self.commandReceiverIsRunning = false
                    return
                }
                
                let cmdType = CommandTypes(rawValue: Int(buffer[0]))
                print("read command type " + String(buffer[0]))
                
                if (cmdType == nil)
                {
                    self.commandReceiverIsRunning = false
                    return
                }
                
                let cmd = CommandFactory.build(type: cmdType!)
                
                if (cmd.cmdtype != CommandTypes.NONE)
                {
                    print("received command " + cmd.cmdtype.description)
                    cmd.receive(comm: self)
                    
                    DispatchQueue.global(qos: .userInitiated).async
                    {
                        for listener: CommandListener? in self.commandListeners
                        {
                            listener?(cmd)
                        }
                        
                        let idx:String = self.getCommandIndex(cmd)
                        let str: CommandResponseListenerStr? = self.commandResponseListeners[idx]
                        if (str != nil)
                        {
                            str?.command.onResponse(command: cmd)
                            str?.listener(str?.command, cmd)
                        }
                    }
                }
            }
        }
    }
    
    private var commandSenderCondition = NSCondition()
    private var commandSenderIsRunning:Bool = false
    private var commandQueue = [Command]()
    private var commandResponseListeners = [String: CommandResponseListenerStr]()
    private var commandListeners = [CommandListener?]()
    
    func addCommandListener(weak listener:@escaping CommandListener)
    {
        commandListeners.append(listener)
    }
    
    private func getCommandIndex(_ command:Command) -> String
    {
        return String(command.id) + "-" + String(command.cmdtype.rawValue)
    }
    
    func sendCommand(command:Command, listener: @escaping CommandResponseListener = {(foo:Command?, bar:Command) -> Void in})
    {
        if (!commandSenderIsRunning)
            { return }
        
        let idx = getCommandIndex(command)
        commandResponseListeners[idx] = CommandResponseListenerStr(
            index: idx, command: command, listener: listener
        )
        
        print("command send lock: acquiring")
        commandSenderCondition.lock()
            print("command send lock: acquiured")
            commandQueue.append(command)
            print("enqueued command: " + command.cmdtype.description)
            commandSenderCondition.signal()
        commandSenderCondition.unlock()
        print("command send lock: released")
    }
    
    func startCommandSender()
    {
        if (commandSenderIsRunning)
        { return }
        
        DispatchQueue.global(qos:.userInitiated).async
        {
            if (self.commandSenderIsRunning)
                { return }
            
            self.commandQueue = [Command]()
            self.commandSenderIsRunning = true
            
            print("command sender: started")
            while (self.commandSenderIsRunning)
            {
                var nextCommand:Command? = nil
                print("command sender lock: acquiring")
                self.commandSenderCondition.lock()
                
                    print("command sender lock: acquired")
                    if (self.commandQueue.count == 0)
                    {
                        print("command sender lock: waiting")
                        self.commandSenderCondition.wait()
                        print("command sender lock: signal received")
                    }
                
                    if (self.commandSenderIsRunning && self.commandQueue.count > 0)
                        { nextCommand = self.commandQueue.remove(at:0) }
                
                self.commandSenderCondition.unlock()
                print("command sender lock: released")
                
                if (nextCommand != nil)
                {
                    print("command sender: sending command " + nextCommand!.cmdtype.description)
                    nextCommand?.send(comm: self)
                }
            }
        }
    }
}

typealias CommandListener = (_ command:Command) -> Void
typealias CommandResponseListener = (_ origCmd: Command?, _ respCmd: Command) -> Void

struct CommandResponseListenerStr
{
    var index:String
    var command:Command
    var listener: CommandResponseListener
}

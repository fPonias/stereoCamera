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
        disconnect()
    }
    
    var _isMaster:Bool = false
    var isMaster:Bool
    {
        get
        {
            return _isMaster
        }
    }
    
    var _address:String = ""
    var address:String
    {
        get
        {
            return _address
        }
    }
    
    private let connectCnd = NSCondition()
    private var isConnecting = false;
    
    func disconnect()
    {
        print("disconnect triggered")
        connectCnd.lock()
            if (isConnecting || commIsConnected(self.cppPtr) != 0)
            {
                print("disconnecting")
                
                commCleanUp(self.cppPtr)
                self.cppPtr = commNew()

                self.isConnecting = false
                
                connectCnd.signal()
            }
        connectCnd.unlock()
        
        killProcessors()
    }
    
    private func killProcessors()
    {
        if (commandReceiverIsRunning)
        {
            commandsReceivedCnd.lock()
                commandReceiverIsRunning = false;
                commandsReceivedCnd.signal()
            commandsReceivedCnd.unlock()
            
            commandRepliesCnd.lock()
                commandRepliesCnd.signal()
            commandRepliesCnd.lock()
        }
        
        if(commandSenderIsRunning)
        {
            commandSenderCondition.lock()
                commandSenderIsRunning = false
                commandSenderCondition.signal()
            commandSenderCondition.unlock()
        }
    }
    
    func connect(onConnected connected: @escaping (_ client: String) -> Void, onFail fail: @escaping () -> Void, timeout:TimeInterval = 2.5)
    {
        let guess = CommManager.instance.guessAddress()
        connect(master: guess.isMaster, address: guess.address, onConnected: connected, onFail: fail, timeout: timeout)
    }
    
    func connect(master:Bool, address:String, onConnected connected: @escaping (_ client: String) -> Void, onFail fail: @escaping () -> Void, timeout:TimeInterval = 2.5)
    {
        var doReturn = false
        connectCnd.lock()
            if (isConnecting || commIsConnected(self.cppPtr) > 0)
                { doReturn = true }
            else
                { isConnecting = true }
        connectCnd.unlock()
        
        if (doReturn)
            { return }
        
        _isMaster = master
        _address = address
        
        DispatchQueue.global(qos: .userInitiated).async
        { [unowned self] in
            self.connectThread(master: master, address: address, onConnected: connected, onFail: fail)
        }
        
        DispatchQueue.global(qos: .userInitiated).async
        { [unowned self] in
            self.connectCnd.lock()
                self.connectCnd.wait(until: Date(timeIntervalSinceNow: timeout))
            
                if (commIsConnected(self.cppPtr) == 0)
                {
                    print("connect timed out")
                    
                    commCleanUp(self.cppPtr)
                    self.cppPtr = commNew()
                    
                    self.isConnecting = false
                }
            self.connectCnd.unlock()
        }
    }
    
    func connectThread(master:Bool, address:String, onConnected connected: @escaping (_ client: String) -> Void, onFail fail: @escaping () -> Void)
    {
        if (master)
            { commStartServer(cppPtr, CommManager.PORT) }
        else
            { commStartClient(cppPtr, address, CommManager.PORT) }
        
        var doReturn = false
        connectCnd.lock()
            if (!isConnecting || commIsConnected(cppPtr) == 0)
                { doReturn = true }
            else
            {
                isConnecting = false
                connectCnd.signal()
            }
        connectCnd.unlock()
        
        if (doReturn)
        {
            print("connect failed")
            fail()
            return
        }
        
        print("comm connected starting sender and receiver")
        startCommandSender()
        startCommandReceiver()
        
        while (!self.commandReceiverIsRunning && !self.commandSenderIsRunning)
            { usleep(100000) }
        
        print("comm connected")
        connected(address)
    }
    
    func read(buffer:[UInt8]) -> Int32
    {
        let bufPtr = UnsafeMutablePointer<UInt8>(mutating: buffer)
        let szRead = commRead(cppPtr, bufPtr, Int32(buffer.count))
        
        return szRead
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
    private let commandsReceivedCnd = NSCondition()
    private var commandsReceivedQueue = [Command]()
    private let commandRepliesCnd = NSCondition()
    private var commandRepliesQueue = [Command]()
    
    func startCommandReceiver()
    {
        if (self.commandReceiverIsRunning)
        {return}
        
        DispatchQueue.global(qos: .userInitiated).async
        { [unowned self] in
            if (self.commandReceiverIsRunning)
                {return}
            
            print("starting command receiver")
            self.commandReceiverIsRunning = true
            
            self.startCommandsReceivedProcessor()
            self.startCommandRepliesProcessor()
            
            let buffer = [UInt8].init(repeating: 0, count: 1)
            
            while(self.commandReceiverIsRunning)
            {
                self.receiveCommand(buffer: buffer)
            }
        }
    }
    
    private func receiveCommand(buffer:[UInt8])
    {
        let bufferPtr = UnsafeMutablePointer<UInt8>(mutating: buffer)
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
            cmd.receive(comm: self)
            
            if (cmd.isResponse)
            {
                print("reply: enqueueing response " + cmd.cmdtype.description)
                self.commandRepliesCnd.lock()
                    self.commandRepliesQueue.append(cmd)
                    self.commandRepliesCnd.signal()
                self.commandRepliesCnd.unlock()
            }
            else
            {
                print("received: enqueueing command " + cmd.cmdtype.description)
                self.commandsReceivedCnd.lock()
                    self.commandsReceivedQueue.append(cmd)
                    self.commandsReceivedCnd.signal()
                self.commandsReceivedCnd.unlock()
            }
        }
    }
    
    private func startCommandRepliesProcessor()
    {
        DispatchQueue.global(qos: .userInitiated).async
        { [unowned self] in
            while (self.commandReceiverIsRunning)
            {
                self.processCommandReply()
            }
        }
    }
    
    private func processCommandReply()
    {
        var cmd:Command? = nil
        
        self.commandRepliesCnd.lock()
            if (self.commandRepliesQueue.count == 0)
            {
                print ("reply: response processor waiting")
                self.commandRepliesCnd.wait()
            }
        
            if (self.commandRepliesQueue.count > 0)
            {
                cmd = self.commandRepliesQueue.removeFirst()
            }
        self.commandRepliesCnd.unlock()
        
        if (cmd == nil)
            { return }
        
        let idx:String = self.getCommandIndex(cmd!)
        let str: CommandResponseListenerStr? = self.commandResponseListeners[idx]
        if (str != nil)
        {
            print ("reply: handling command " + cmd!.cmdtype.description)
            str?.command.onResponse(command: cmd!)
            str?.listener(str?.command, cmd!)
        }
    }
    
    private func startCommandsReceivedProcessor()
    {
        DispatchQueue.global(qos: .userInitiated).async
        {  [unowned self] in
            while (self.commandReceiverIsRunning)
            {
                self.processReceivedCommand()
            }
        }
    }
    
    private func processReceivedCommand()
    {
        var cmd:Command? = nil
        
        self.commandsReceivedCnd.lock()
            if (self.commandsReceivedQueue.count == 0 || self.commandListener == nil)
            {
                print ("received: processor waiting")
                self.commandsReceivedCnd.wait()
            }

            if (self.commandsReceivedQueue.count > 0 && self.commandListener != nil)
                { cmd = self.commandsReceivedQueue.removeFirst() }
        self.commandsReceivedCnd.unlock()

        if (!self.commandReceiverIsRunning || cmd == nil)
            { return }

        print ("received: handling command " + cmd!.cmdtype.description)
        
        self.commandListener?.onCommand(cmd!)
    }
    
    private var commandSenderCondition = NSCondition()
    private var commandSenderIsRunning:Bool = false
    private var commandQueue = [Command]()
    private var commandResponseListeners = [String: CommandResponseListenerStr]()
    private weak var _commandListener:CommandListener? = nil
    
    var commandListener:CommandListener?
    {
        get { return _commandListener }
        
        set
        {
            self.commandsReceivedCnd.lock()
                _commandListener = newValue
            
                if (_commandListener != nil)
                {
                    print("command listener set")
                    self.commandsReceivedCnd.signal()
                }
                else
                    { print ("command listener unset" )}
            self.commandsReceivedCnd.unlock()
        }
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
        
        commandSenderCondition.lock()
            commandQueue.append(command)
            print("send: enqueueing command: " + command.cmdtype.description)
            commandSenderCondition.signal()
        commandSenderCondition.unlock()
    }
    
    func startCommandSender()
    {
        if (commandSenderIsRunning)
        { return }
        
        DispatchQueue.global(qos:.userInitiated).async
        { [unowned self] in
            if (self.commandSenderIsRunning)
                { return }
            
            self.commandSenderIsRunning = true
            
            print("send: started")
            while (self.commandSenderIsRunning)
            {
                self.sendCommand()
            }
        }
    }
    
    private func sendCommand()
    {
        var nextCommand:Command? = nil
        self.commandSenderCondition.lock()

            if (self.commandQueue.count == 0)
            {
                print("send: waiting")
                self.commandSenderCondition.wait()
            }

            if (self.commandSenderIsRunning && self.commandQueue.count > 0)
                { nextCommand = self.commandQueue.remove(at:0) }

        self.commandSenderCondition.unlock()

        if (nextCommand != nil)
        {
            print("send: sending command " + nextCommand!.cmdtype.description)
            nextCommand?.send(comm: self)
        }
    }
}

protocol CommandListener: class
{
    func onCommand(_ command:Command) -> Void
}

typealias CommandResponseListener = (_ origCmd: Command?, _ respCmd: Command) -> Void

struct CommandResponseListenerStr
{
    var index:String
    var command:Command
    var listener: CommandResponseListener
}

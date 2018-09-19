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
    init(master:Bool)
    {
        _isMaster = master
    }

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
        killProcessors()
    
        connectCnd.lock()
            if (isConnecting || commIsConnected(self.cppPtr) != 0)
            {
                print("disconnecting")
                
                commCleanUp(self.cppPtr)
                //self.cppPtr = commNew()

                self.isConnecting = false
                
                connectCnd.signal()
            }
        connectCnd.unlock()
    }
    
    func isConnected() -> Bool
    {
        let ret = commIsConnected(self.cppPtr)
        return (ret != 0) ? true : false
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
            commandRepliesCnd.unlock()
            
            commandResponseCondition.lock()
                commandResponseCondition.signal()
            commandResponseCondition.unlock()
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
        connect(address: guess.address, onConnected: connected, onFail: fail, timeout: timeout)
    }
    
    func connect(address:String, onConnected connected: @escaping (_ client: String) -> Void, onFail fail: @escaping () -> Void, timeout:TimeInterval = 2.5)
    {
        var doReturn = false
        connectCnd.lock()
            if (isConnecting)
            {
                if (commIsConnected(self.cppPtr) == 0)
                {
                    self.onConnected = connected
                    self.onConnectFail = fail
                }
                else
                {
                    connected(_address)
                }
                
                doReturn = true
            }
            else
            {
                isConnecting = true
                self.onConnected = connected
                self.onConnectFail = fail
            }
        connectCnd.unlock()
        
        if (doReturn)
            { return }
        
        _address = address
        
        Thread.detachNewThread
        { [unowned self] in
            Thread.current.name = "Connect Thread"
            self.connectThread(address: address)
        }
        
        if (timeout > 0)
        {
            DispatchQueue.global(qos: .userInitiated).async
            { [unowned self] in
                self.connectCnd.lock()
                    self.connectCnd.wait(until: Date(timeIntervalSinceNow: timeout))
                
                    if (commIsConnected(self.cppPtr) == 0)
                    {
                        print("connect timed out")
                        
                        commCleanUp(self.cppPtr)
                        //self.cppPtr = commNew()
                        
                        self.isConnecting = false
                        fail()
                    }
                self.connectCnd.unlock()
            }
        }
    }
    
    private var onConnected:Optional<(_ client: String) -> Void> = nil
    private var onConnectFail:Optional<() -> Void> = nil
    
    func connectThread(address:String)
    {
        if (_isMaster)
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
            onConnectFail?()
            return
        }
        
        print("comm connected starting sender and receiver")
        startCommandSender()
        startCommandReceiver()
        startCommandTimeoutThread()
        
        while (!self.commandReceiverIsRunning && !self.commandSenderIsRunning)
            { usleep(100000) }
        
        print("comm connected")
        onConnected?(address)
    }
    
    func read(buffer:[UInt8]) -> Int32
    {
        let bufPtr = UnsafeMutablePointer<UInt8>(mutating: buffer)
        let sz = commRead(cppPtr, bufPtr, Int32(buffer.count))
        
        if (sz <= 0 || !commandReceiverIsRunning)
        {
            commandListener?.onDisconnect()
        }
        
        return sz
    }
    
    func read(sz:Int) -> ([UInt8], Int32)
    {
        let buf = [UInt8](repeating: 0, count: sz)
        let bufPtr = UnsafeMutablePointer<UInt8>(mutating: buf)
        let sz = commRead(cppPtr, bufPtr, Int32(sz))
        
        if (sz <= 0 || !commandReceiverIsRunning)
        {
            commandListener?.onDisconnect()
        }
        
        return (buf, sz)
    }
    
    func write(buf:[UInt8]) -> Int32
    {
        let sz = buf.count
        let bufPtr = UnsafePointer<UInt8>(buf)
        let szWritten = commWrite(cppPtr, bufPtr, Int32(sz))
        
        if (szWritten <= 0 || !commandReceiverIsRunning)
        {
            commandListener?.onDisconnect()
        }
        
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
        
        Thread.detachNewThread
        { [unowned self] in
            if (self.commandReceiverIsRunning)
                {return}
            
            Thread.current.name = "Command Receiver"
            print("starting command receiver")
            self.commandReceiverIsRunning = true
            
            self.startCommandsReceivedProcessor()
            self.startCommandRepliesProcessor()
            
            let buffer = [UInt8].init(repeating: 0, count: 1)
            Thread.current.qualityOfService = QualityOfService.userInteractive
            
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
            self.commandListener?.onDisconnect()
            disconnect()
            usleep(10000)
            return
        }

        let cmdType = CommandTypes(rawValue: Int(buffer[0]))
        print("read command type " + String(buffer[0]))

        if (cmdType == nil)
        {
            self.commandListener?.onDisconnect()
            return
        }

        let cmd = CommandFactory.build(type: cmdType!)

        if (cmd.cmdtype != CommandTypes.NONE)
        {
            
            let success = cmd.receive(comm: self)
            var str:CommandResponseListenerStr? = nil
            
            commandResponseCondition.lock()
                let idx:String = self.getCommandIndex(cmd)
                str = self.commandResponseListeners[idx]
            commandResponseCondition.unlock()
            
            if (!success)
            {
                str?.listener.onReceiveFailed(cmd)
                return
            }
            else
            {
                str?.listener.onReceived(cmd, origCommand: str?.command)
            }
            
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
    
    private func startCommandTimeoutThread()
    {
        Thread.detachNewThread
        { [unowned self] in
            Thread.current.name = "Command Timeout Checker"
            while (self.commandReceiverIsRunning)
            {
                self.checkTimeouts()
            }
        }
    }
    
    private func checkTimeouts()
    {
        var smallestWait = -1.0
        commandResponseCondition.lock()
            var doWait = true
            for key in commandResponseListeners.keys
            {
                let listener = commandResponseListeners[key]
                
                if (listener != nil && listener!.start > 0)
                {
                    doWait = false
                    break
                }
            }
        
            if (doWait)
            {
                commandResponseCondition.wait()
            }
        
            print("checking timeouts")
        
            var events = [CommandResponseListenerStr]()
            let now = Date().timeIntervalSince1970
            for key in commandResponseListeners.keys
            {
                let listener = commandResponseListeners[key]
                
                if (listener != nil && listener!.start > 0)
                {
                    let then = listener!.start + listener!.timeout
                    let diff = then - now
                    
                    if (diff <= 0)
                    {
                        events.append(listener!)
                        commandResponseListeners.removeValue(forKey: key)
                    }
                    else
                    {
                        if (smallestWait < 0 || diff < smallestWait)
                            { smallestWait = diff }
                    }
                }
            }
        commandResponseCondition.unlock()
        
        if (!commandReceiverIsRunning)
            { return }
        
        for listener in events
        {
            print("command " + listener.command.cmdtype.description + " timed out")
            listener.listener.onReceiveFailed(listener.command)
        }
        
        if (smallestWait > 0)
        {
            print("waiting " + String(smallestWait) + " for next possible timeout")
            commandResponseCondition.lock()
                let until = Date(timeIntervalSinceNow: smallestWait)
                commandResponseCondition.wait(until: until)
            commandResponseCondition.unlock()
        }
    }
    
    private func startCommandRepliesProcessor()
    {
        Thread.detachNewThread
        { [unowned self] in
            Thread.current.name = "Command Reply Processor"
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
        
        self.commandResponseCondition.lock()
            let idx:String = self.getCommandIndex(cmd!)
            let str: CommandResponseListenerStr? = self.commandResponseListeners[idx]
            self.commandResponseListeners.removeValue(forKey: idx)
        self.commandResponseCondition.unlock()
    
        if (str != nil)
        {
            print ("reply: handling command " + cmd!.cmdtype.description)
            str?.command.onResponse(command: cmd!)
        }
    }
    
    private func startCommandsReceivedProcessor()
    {
        Thread.detachNewThread
        {  [unowned self] in
            Thread.current.name = "Commands Received Processor"
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
    private var commandResponseCondition = NSCondition()
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
    
    class DefaultCommandResponseListener : CommandResponseListener
    {
        var listener:(Bool, Command, Command?) -> Void
        init(_ listener:@escaping (Bool, Command, Command?) -> Void)
        {
            self.listener = listener
        }
    
        override func onReceived(_ command: Command, origCommand: Command?)
        {
            listener(true, command, origCommand)
        }
        
        override func onReceiveFailed(_ command: Command)
        {
            listener(false, command, nil)
        }
    }
    
    func sendCommand(command:Command, listenerFunc: @escaping (Bool, Command, Command?) -> Void)
    {
        sendCommand(command: command, listener: DefaultCommandResponseListener(listenerFunc))
    }
    
    func sendCommand(command:Command, listener: CommandResponseListener? = nil, timeout:Double = 5.0)
    {
        if (!commandSenderIsRunning)
            { return }
        
        let idx = getCommandIndex(command)
        
        if (listener != nil)
        {
            commandResponseCondition.lock()
                commandResponseListeners[idx] = CommandResponseListenerStr(
                    index: idx, command: command, listener: listener!, start: 0, timeout: timeout
                )
                commandResponseCondition.signal()
            commandResponseCondition.unlock()
        }
        
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
        
        Thread.detachNewThread
        { [unowned self] in
            if (self.commandSenderIsRunning)
                { return }
            
            Thread.current.name = "Command sender"
            self.commandSenderIsRunning = true
            
            print("send: started")
            Thread.current.qualityOfService = QualityOfService.userInteractive
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
            commandResponseCondition.lock()
                let idx:String = self.getCommandIndex(nextCommand!)
                var str: CommandResponseListenerStr? = self.commandResponseListeners[idx]
                str?.start = Date().timeIntervalSince1970
            
                commandResponseCondition.signal()
            commandResponseCondition.unlock()
        
            print("send: sending command " + nextCommand!.cmdtype.description)
            let success = nextCommand!.send(comm: self)
            
            if (!success)
                { str?.listener.onSendFailed(nextCommand!) }
            else
                { str?.listener.onSent(nextCommand!)}
        }
    }
}

protocol CommandListener: class
{
    func onCommand(_ command:Command) -> Void
    func onDisconnect() -> Void
}

class CommandResponseListener
{
    var timeout:Float = 0.0

    func onReceivingStarted(_ command:Command) {}
    func onReceiving(_ command:Command, origCommand:Command?, progress:Float) {}
    func onReceived(_ command:Command, origCommand:Command?) {}
    func onReceiveFailed(_ command:Command) {}
    func onSendingStarted(_ command:Command) {}
    func onSending(_ command:Command, progress:Float) {}
    func onSent(_ command:Command) {}
    func onSendFailed(_ command:Command) {}
}

struct CommandResponseListenerStr
{
    var index:String
    var command:Command
    var listener: CommandResponseListener
    var start:Double
    var timeout:Double
}

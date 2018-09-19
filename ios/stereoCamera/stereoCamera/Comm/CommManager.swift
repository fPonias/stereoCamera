//
//  CommManager.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/13/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation
import UIKit

class CommManager
{
    static var PORT:UInt32 = 3981
    
    private static var _instance:CommManager = CommManager()
    static var instance:CommManager
    {
        get { return _instance }
    }
    
    private var _localAddresses:[String] = [String]()
    var localAddresses:[String]
    {
        get { return _localAddresses }
    }
    
    private var _commServer:Comm = Comm(master: true)
    var commServer:Comm
    {
        get { return _commServer }
    }
    
    private var _commClient:Comm = Comm(master: false)
    var commClient:Comm
    {
        get { return _commClient }
    }
    
    var comm:Comm
    {
        get
        {
            if (_commClient.isConnected())
                { return _commClient }
            else
                { return _commServer }
        }
    }
    
    private let _slaveState:SlaveState = SlaveState()
    var slaveState:SlaveState
    {
        get { return _slaveState }
    }
    
    var isMaster:Bool
    {
        get
        {
            return (comm != nil && comm === _commServer)
        }
    }
    
    class AppListener : UIResponder, UIApplicationDelegate
    {
        private var _parent:CommManager
        init (parent: CommManager)
        {
            _parent = parent
        }
        
        func applicationDidBecomeActive(_ application: UIApplication)
        {
            _parent.fetchLocalAddresses()
        }
        
        func applicationWillTerminate(_ application: UIApplication)
        {
            _parent._commClient.disconnect()
            _parent._commServer.disconnect()
        }
    }
    private var listener:AppListener? = nil
    
    private init()
    {
        listener = AppListener(parent: self)
        AppDelegate.instance?.addListener(listener!)
        fetchLocalAddresses()
    }
    
    func fetchLocalAddresses()
    {
        _localAddresses.removeAll()
        let addresses:[IFAddress] = getIFAddresses()
        
        for address:IFAddress in addresses
        {
            let parts:[Substring] = address.address.split(separator: ".")
            if (parts.count == 4)
            {
                _localAddresses.append(address.address)
            }
        }
    }

    struct AddressGuess
    {
        var isMaster:Bool = false
        var address:String = ""
    }

    func guessAddress() -> AddressGuess
    {
        let addresses = getIFAddresses()
        var ret = AddressGuess()
        
        for address:IFAddress in addresses
        {
            var parts:[Substring] = address.address.split(separator: ".")
            if (parts.count == 4)
            {
                if (parts[3] == "1")
                {
                    ret.isMaster = true
                    ret.address = address.address
                    break
                }
                else
                {
                    parts[3] = "1"
                    ret.isMaster = false;
                    ret.address = parts.joined(separator:".")
                    
                    if (parts[0] == "172" && parts[1] == "20" && parts[2] == "10") //if connected to an apple network tether
                    {
                        break
                    }
                    else if (parts[0] == "192" && parts[1] == "168") //if connected to a default home router setup
                    {
                        break
                    }
                }
            }
        }
        
        return ret
    }
    
    struct IFAddress
    {
        var address:String = ""
        var name:String = ""
    }
    
    private func getIFAddresses() -> [IFAddress]
    {
        var addresses = [IFAddress]()
        
        // Get list of all interfaces on the local machine:
        var ifaddr : UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return [] }
        guard let firstAddr = ifaddr else { return [] }
        
        // For each interface ...
        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next })
        {
            let flags = Int32(ptr.pointee.ifa_flags)
            let addr = ptr.pointee.ifa_addr.pointee
            let name = ptr.pointee.ifa_name
            
            // Check for running IPv4, IPv6 interfaces. Skip the loopback interface.
            if (flags & (IFF_UP|IFF_RUNNING|IFF_LOOPBACK)) == (IFF_UP|IFF_RUNNING)
            {
                if addr.sa_family == UInt8(AF_INET) || addr.sa_family == UInt8(AF_INET6)
                {
                    // Convert interface address to a human readable string:
                    var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                    let nameinfo = getnameinfo(ptr.pointee.ifa_addr, socklen_t(addr.sa_len), &hostname, socklen_t(hostname.count), nil, socklen_t(0), NI_NUMERICHOST)
                    if (nameinfo == 0)
                    {
                        let address = String(cString: hostname)
                        let addressStr = IFAddress(address: address, name: String(cString: name!))
                        addresses.append(addressStr)
                    }
                }
            }
        }
        
        freeifaddrs(ifaddr)
        return addresses
    }
}

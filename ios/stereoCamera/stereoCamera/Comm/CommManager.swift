//
//  CommManager.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/13/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class CommManager
{
    static var PORT:UInt32 = 3981
    
    private static var _instance:CommManager = CommManager()
    static var instance:CommManager
    {
        get { return _instance }
    }
    
    private var _isMaster:Bool = false
    var isMaster:Bool
    {
        get { return _isMaster }
    }

    private var _masterAddresses:[String] = [String]()
    var masterAddresses:[String]
    {
        get { return _masterAddresses }
    }
    
    private var _comm:Comm = Comm()
    var comm:Comm
    {
        get { return _comm }
    }
    
    init()
    {
        guessMasterAddresses()
    }
    
    private func guessMasterAddresses()
    {
        let addresses:[String] = getIFAddresses()
        
        for address:String in addresses
        {
            var parts:[Substring] = address.split(separator: ".")
            if (parts.count == 4)
            {
                if (parts[3] == "1")
                {
                    _isMaster = true
                    _masterAddresses.removeAll(keepingCapacity: false)
                    _masterAddresses.append(address)
                    return;
                }
                
                parts[3] = "1"
                _masterAddresses.append(parts.joined(separator:"."))
            }
        }
        
        _isMaster = false
    }
    
    private func getIFAddresses() -> [String]
    {
        var addresses = [String]()
        
        // Get list of all interfaces on the local machine:
        var ifaddr : UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return [] }
        guard let firstAddr = ifaddr else { return [] }
        
        // For each interface ...
        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next })
        {
            let flags = Int32(ptr.pointee.ifa_flags)
            let addr = ptr.pointee.ifa_addr.pointee
            
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
                        addresses.append(address)
                    }
                }
            }
        }
        
        freeifaddrs(ifaddr)
        return addresses
    }
}

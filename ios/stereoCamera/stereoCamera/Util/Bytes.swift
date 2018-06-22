//
//  Bytes.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/16/18.
//  Copyright © 2018 cody. All rights reserved.
//

import Foundation

class Bytes
{
    static func toByteArray<T>(_ value: T) -> [UInt8]
    {
        var value = value
        return withUnsafePointer(to: &value)
        {
            $0.withMemoryRebound(to: UInt8.self, capacity: MemoryLayout<T>.size)
            {
                Array(UnsafeBufferPointer(start: $0, count: MemoryLayout<T>.size))
            }
        }
    }
    
    static func fromByteArray<T>(_ value: [UInt8], _ offset: Int = 0) -> T
    {
        return value.withUnsafeBufferPointer
            {
                $0.baseAddress!.advanced(by: offset).withMemoryRebound(to: T.self, capacity: 1)
                {
                    $0.pointee
                }
        }
    }
    
    static func toPointer(_ str:String) -> UnsafePointer<UInt8>?
    {
        guard let data = str.data(using: String.Encoding.utf8) else { return nil }
        
        let buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: data.count + 1)
        let stream = OutputStream(toBuffer: buffer, capacity: data.count + 1)
        
        stream.open()
        data.withUnsafeBytes({ (p: UnsafePointer<UInt8>) -> Void in
            stream.write(p, maxLength: data.count)
        })
        stream.close()
        
        //swift doesn't know that C expects null termination on its strings
        buffer.advanced(by: data.count).assign(repeating: 0, count: 1)
        
        return UnsafePointer<UInt8>(buffer)
    }
}

//
//  OffsetFinder.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/25/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import MetalKit
import Metal

class OffsetFinder
{
    private var _textureA:MTLTexture?
    private var _textureB:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var _device:MTLDevice?
    private var state:MTLComputePipelineState?
    private var argsBuf:MTLBuffer?
    private var argsPtr:UnsafeMutablePointer<Int32>?
    private var diffBuf:MTLBuffer?
    private var diffPtr:UnsafeMutablePointer<Int32>?
    
    
    init()
    {
        if (_device == nil) {
            _device = MTLCreateSystemDefaultDevice()
        }
        
        guard let device = _device,
              CVMetalTextureCacheCreate(kCFAllocatorDefault, nil, device, nil, &_textureCache) == kCVReturnSuccess
        else { return }
        
        guard let library = device.makeDefaultLibrary() else { return }
        guard let fcn = library.makeFunction(name: "difference") else { return }
        do {
            state = try device.makeComputePipelineState(function: fcn)
        }
        catch {
            return
        }
        
        let sizeof = MemoryLayout<Int32>.size
        diffBuf = device.makeBuffer(length: sizeof, options: .storageModeShared)
        //argsBuf = device.makeBuffer(length: <#T##Int#>, options: <#T##MTLResourceOptions#>)
        diffPtr = diffBuf?.contents().bindMemory(to: Int32.self, capacity: 1)
        argsPtr = argsBuf?.contents().bindMemory(to: Int32.self, capacity: 4)
    }
    
    
}

//
//  Histogram.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import MetalKit

class Histogram
{
    private var _texture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var _device:MTLDevice?
    private var PSO:MTLComputePipelineState?
    private var marginBuf:MTLBuffer?
    private var resultBuf:MTLBuffer?
    private var histogramPtr:UnsafeMutablePointer<Int32>?
    private let SZ = 512
    private var width:Int = 0
    private var height:Int = 0
    
    init()
    {
        if (_device == nil) {
            _device = MTLCreateSystemDefaultDevice()
        }
        
        guard let device = _device,
              CVMetalTextureCacheCreate(kCFAllocatorDefault, nil, device, nil, &_textureCache) == kCVReturnSuccess
        else { return }
        
        guard let library = device.makeDefaultLibrary() else { return }
        guard let fcn = library.makeFunction(name: "histogram") else { return }
        do {
            PSO = try device.makeComputePipelineState(function: fcn)
        }
        catch {
            return
        }
        
        let sizeof = MemoryLayout<Int32>.size
        resultBuf = device.makeBuffer(length: SZ * sizeof, options: .storageModeShared)
        histogramPtr = resultBuf?.contents().bindMemory(to: Int32.self, capacity: SZ)
        
        marginBuf = device.makeBuffer(length: 4 * sizeof, options: .storageModeShared)
        marginBuf?.contents().initializeMemory(as: Int32.self, repeating: 0, count: 4)
    }
    
    struct TextureMargin {
        var left:Int
        var top:Int
        var right:Int
        var bottom:Int
    }
    
    private var margins = TextureMargin(left: 0, top: 0, right: 0, bottom: 0)
    
    func setMargins(_ margins:TextureMargin) {
        self.margins = margins
        guard let ptr = marginBuf?.contents() else { return }
        
        let sizeof = MemoryLayout<Int32>.size
        ptr.storeBytes(of: Int32(margins.left), toByteOffset: 0, as: Int32.self)
        ptr.storeBytes(of: Int32(margins.top), toByteOffset: sizeof, as: Int32.self)
        ptr.storeBytes(of: Int32(margins.right), toByteOffset: sizeof * 2, as: Int32.self)
        ptr.storeBytes(of: Int32(margins.bottom), toByteOffset: sizeof * 3, as: Int32.self)
    }
    
    func setPixels(pixels:CVImageBuffer)
    {
        width = CVPixelBufferGetWidth(pixels)
        height = CVPixelBufferGetHeight(pixels)
        
        guard let unwrappedCache = _textureCache else { return }
        var texture:CVMetalTexture?
        let status = CVMetalTextureCacheCreateTextureFromImage(kCFAllocatorDefault, unwrappedCache, pixels, nil, MTLPixelFormat.bgra8Unorm, width, height, 0, &texture)
        
        guard (status == kCVReturnSuccess),
              let unwrappedTexture = texture
        else { return }
        _texture = CVMetalTextureGetTexture(unwrappedTexture)
    }
    
    public func hasTexture() -> Bool {
        return _texture != nil
    }
    
    func setZoom(_ zoom:Float)
    {
        let margin:TextureMargin
        
        if (width > height) {
            let trim = (width - height) / 2
            let dim = Int(Float(height) / zoom)
            let diff = (height - dim) / 2
            margin = TextureMargin(
                left: diff + trim,
                top: diff,
                right: diff + trim,
                bottom: diff
            )
        } else {
            let trim = (height - width) / 2
            let dim = Int(Float(width) / zoom)
            let diff = (width - dim) / 2
            margin = TextureMargin(
                left: diff,
                top: diff + trim,
                right: diff,
                bottom: diff + trim
            )
        }
        
        setMargins(margin)
    }
    
    var zoomGranularity:Float{
        get {
            let dim = min(width, height)
            return 1.0 / Float(dim)
        }
    }
    
    var size:Int {
        get {
            let w = width - margins.left - margins.right
            let h = height - margins.top - margins.bottom
            
            return w * h
        }
    }
    
    func calculate() -> [Int32]? {
        guard let device = _device,
              let resultBuf = resultBuf,
              let histogramPtr = histogramPtr,
              let PSO = PSO
        else { return nil }
            
        histogramPtr.assign(repeating: 0, count: SZ)
                
        guard let queue = device.makeCommandQueue(),
              let cmdBuf = queue.makeCommandBuffer(),
              let encoder = cmdBuf.makeComputeCommandEncoder()
        else { return nil }
        
        encoder.setComputePipelineState(PSO)
        encoder.setTexture(_texture, index: 0)
        
        encoder.setBuffer(resultBuf, offset: 0, index: 0)
        encoder.setBuffer(marginBuf, offset: 0, index: 1)
        
        let gridSize = MTLSizeMake(width, height, 1)
        let groupSize = MTLSizeMake(PSO.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.endEncoding()
        cmdBuf.commit()
        cmdBuf.waitUntilCompleted()
        
        return Array(UnsafeBufferPointer(start: histogramPtr, count: SZ))
    }
}

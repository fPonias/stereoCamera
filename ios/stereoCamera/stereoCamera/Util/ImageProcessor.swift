//
//  ImageProcessor.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/22/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import MetalKit
import CoreImage
import Photos

class ImageProcessor {
    private(set) var _inTexture:MTLTexture?
    private(set) var _midTexture:MTLTexture?
    private(set) var _outTexture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var _device:MTLDevice?
    private var cropState:MTLComputePipelineState?
    private var maskState:MTLComputePipelineState?
    private var autoContrastState:MTLComputePipelineState?
    
    private var offsetBuf:MTLBuffer?
    private var offsetPtr:UnsafeMutablePointer<Int32>?
    
    var outSize = ImageUtils.Size(width: 0, height: 0)
    private var maskBuf:MTLBuffer?
    var maskPtr:UnsafeMutablePointer<Float32>?
    private var outOffsetBuf:MTLBuffer?
    var outOffsetPtr:UnsafeMutablePointer<UInt32>?
    private var contrastArgsBuf:MTLBuffer?
    var contrastArgsPtr:UnsafeMutablePointer<Float32>?
    
    
    

    init(size:ImageUtils.Size)
    {
        if (_device == nil) {
            _device = MTLCreateSystemDefaultDevice()
        }
        
        guard let device = _device,
              CVMetalTextureCacheCreate(kCFAllocatorDefault, nil, device, nil, &_textureCache) == kCVReturnSuccess
        else { return }
        
        guard let library = device.makeDefaultLibrary() else { return }
        guard let cropfcn = library.makeFunction(name: "crop") else { return }
        guard let maskfcn = library.makeFunction(name: "colorMask") else { return }
        guard let autoContrastFcn = library.makeFunction(name: "autoContrast") else { return }
        do {
            cropState = try device.makeComputePipelineState(function: cropfcn)
            maskState = try device.makeComputePipelineState(function: maskfcn)
            autoContrastState = try device.makeComputePipelineState(function: autoContrastFcn)
        }
        catch {
            return
        }
        
        createOutTexture(size)
        
        let intsz = MemoryLayout<Int32>.size
        offsetBuf = device.makeBuffer(length: 3 * intsz, options: .storageModeShared)
        offsetPtr = offsetBuf?.contents().bindMemory(to: Int32.self, capacity: 3)
        
        let floatsz = MemoryLayout<Float32>.size
        maskBuf = device.makeBuffer(length: 3 * floatsz, options: .storageModeShared)
        maskPtr = maskBuf?.contents().bindMemory(to: Float32.self, capacity: 3)
        
        let uintsz = MemoryLayout<UInt32>.size
        outOffsetBuf = device.makeBuffer(length: 4 * uintsz, options: .storageModeShared)
        outOffsetPtr = outOffsetBuf?.contents().bindMemory(to: UInt32.self, capacity: 4)
        
        contrastArgsBuf = device.makeBuffer(length: 2 * floatsz, options: .storageModeShared)
        contrastArgsPtr = contrastArgsBuf?.contents().bindMemory(to: Float32.self, capacity: 2)
    }
    
    private func orientationToRotation(_ orient: ImageUtils.CameraOrientation) -> Int32 {
        switch (orient) {
        case .DEG_0:  return 2
        case .DEG_90: return 0
        case .DEG_270: return 3
        default: return 1
        }
    }
    
    func setPixels(pixels:CVImageBuffer) {
        let w = CVPixelBufferGetWidth(pixels)
        let h = CVPixelBufferGetHeight(pixels)
        let margins = ImageUtils.Margin(left: 0, top: 0, right: 0, bottom: 0, width: w, height: h)
        setPixels(pixels: pixels, margins: margins, orientation: .DEG_0)
    }
    
    func setPixels(pixels:CVImageBuffer, margins:ImageUtils.Margin, orientation:ImageUtils.CameraOrientation)
    {        
        createInTexture(pixels)
        
        let w = CVPixelBufferGetWidth(pixels) - margins.left - margins.right
        let h = CVPixelBufferGetHeight(pixels) - margins.top - margins.bottom
        createMidTexture(size: ImageUtils.Size(width: w, height: h))
        
        guard let offsetPtr = offsetPtr else { return }
        offsetPtr.pointee = Int32(margins.left)
        (offsetPtr + 1).pointee = Int32(margins.top)
        let rot = orientationToRotation(orientation)
        print("image processor orientation " + String(rot))
        (offsetPtr + 2).pointee = rot
    }
    
    private func createInTexture(_ pixels:CVImageBuffer)
    {
        let width = CVPixelBufferGetWidth(pixels)
        let height = CVPixelBufferGetHeight(pixels)
        guard let unwrappedCache = _textureCache else { return }
        var texture:CVMetalTexture?
        let status = CVMetalTextureCacheCreateTextureFromImage(kCFAllocatorDefault, unwrappedCache, pixels, nil, MTLPixelFormat.bgra8Unorm, width, height, 0, &texture)
        
        guard (status == kCVReturnSuccess),
              let unwrappedTexture = texture
        else { return }
        _inTexture = CVMetalTextureGetTexture(unwrappedTexture)
    }
    
    private func createMidTexture(size:ImageUtils.Size)
    {
        guard size.width > 0 && size.height > 0 else { return }
        
        let textureDesc = MTLTextureDescriptor()
        textureDesc.pixelFormat = MTLPixelFormat.bgra8Unorm
        textureDesc.width = size.width
        textureDesc.height = size.height
        textureDesc.usage = [.shaderRead, .shaderWrite]
        
        _midTexture = _device?.makeTexture(descriptor: textureDesc)
    }
    
    private func createOutTexture(_ size:ImageUtils.Size)
    {
        let textureDesc = MTLTextureDescriptor()
        textureDesc.pixelFormat = MTLPixelFormat.bgra8Unorm
        textureDesc.width = size.width
        textureDesc.height = size.height
        textureDesc.usage = [.shaderRead, .shaderWrite]
        
        _outTexture = _device?.makeTexture(descriptor: textureDesc)
        
        outSize = size
    }
    
    enum Side {
        case LEFT
        case RIGHT
    }
    
    public func processCurrentInTexture(_ side:Side) {
        print ("using unimplemented version of ImageProcessor")
    }
    
    func calculate() {
        guard let device = _device,
              let cropState = cropState,
              let maskState = maskState,
              let inTexture = _inTexture,
              let outTexture = _outTexture
        else { return }
            
        guard let queue = device.makeCommandQueue(),
              let cmdBuf = queue.makeCommandBuffer(),
              let encoder = cmdBuf.makeComputeCommandEncoder()
        else { return }
        
        encoder.setComputePipelineState(cropState)
        encoder.setTexture(inTexture, index: 0)
        encoder.setTexture(_midTexture, index: 1)
        encoder.setBuffer(offsetBuf, offset: 0, index: 0)
        
        var gridSize = MTLSizeMake(inTexture.width, inTexture.height, 1)
        var groupSize = MTLSizeMake(cropState.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.setComputePipelineState(maskState)
        encoder.setTexture(_midTexture, index: 0)
        encoder.setTexture(outTexture, index: 1)
        encoder.setBuffer(maskBuf, offset: 0, index: 0)
        encoder.setBuffer(outOffsetBuf, offset: 0, index: 1)
        
        gridSize = MTLSizeMake(outTexture.width, outTexture.height, 1)
        groupSize = MTLSizeMake(cropState.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.endEncoding()
        cmdBuf.commit()
        cmdBuf.waitUntilCompleted()
    }
    
    func autoContrast()
    {
        guard let outTexture = _inTexture else { return }
        let hist = Histogram()
        hist.setPixels(mtlPixels: outTexture)
        hist.setMargins(Histogram.TextureMargin(left: 0, top: 0, right: 0, bottom: 0))
        hist.setZoom(1.0)
        let params = hist.autoContrastParams()
        
        guard (params.min > 0.0 || params.max < 1.0) else { return }
        
        guard let device = _device,
              let queue = device.makeCommandQueue(),
              let cmdBuf = queue.makeCommandBuffer(),
              let encoder = cmdBuf.makeComputeCommandEncoder(),
              let autoContrastState = autoContrastState,
              let contrastArgsPtr = contrastArgsPtr
        else { return }
        
        (contrastArgsPtr + 0).pointee = Float(params.a)
        (contrastArgsPtr + 1).pointee = Float(params.b)
        
        encoder.setComputePipelineState(autoContrastState)
        encoder.setTexture(outTexture, index: 0)
        encoder.setBuffer(contrastArgsBuf, offset: 0, index: 0)
        
        let gridSize = MTLSizeMake(outTexture.width, outTexture.height, 1)
        let groupSize = MTLSizeMake(autoContrastState.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.endEncoding()
        cmdBuf.commit()
        cmdBuf.waitUntilCompleted()
    }
    
    public func getOutput() -> CIImage? {
        guard let outTexture = _outTexture else { return nil }
        let ret = CIImage(mtlTexture: outTexture, options: nil)
        
        return ret
    }
}

class ImageProcessorGreenMagenta : ImageProcessor
{
    public override func processCurrentInTexture(_ side: ImageProcessor.Side)
    {
        guard let maskPtr = maskPtr else { return }
        if (side == .LEFT) {
            maskPtr.pointee = 0.0
            (maskPtr + 1).pointee = 1.0
            (maskPtr + 2).pointee = 0.0
        }
        else {
            maskPtr.pointee = 1.0
            (maskPtr + 1).pointee = 0.0
            (maskPtr + 2).pointee = 1.0
        }
        
        guard let outOffsetPtr = outOffsetPtr else { return }
        (outOffsetPtr + 0).pointee = 0
        (outOffsetPtr + 1).pointee = 0
        (outOffsetPtr + 2).pointee = UInt32(outSize.width)
        (outOffsetPtr + 3).pointee = UInt32(outSize.height)
        
        
        calculate()
    }
}

class ImageProcessorRedCyan : ImageProcessor
{
    public override func processCurrentInTexture(_ side: ImageProcessor.Side) {
        guard let maskPtr = maskPtr else { return }
        if (side == .LEFT) {
            maskPtr.pointee = 1.0
            (maskPtr + 1).pointee = 0.0
            (maskPtr + 2).pointee = 0.0
        }
        else {
            maskPtr.pointee = 0.0
            (maskPtr + 1).pointee = 1.0
            (maskPtr + 2).pointee = 1.0
        }
        
        guard let outOffsetPtr = outOffsetPtr else { return }
        (outOffsetPtr + 0).pointee = 0
        (outOffsetPtr + 1).pointee = 0
        (outOffsetPtr + 2).pointee = UInt32(outSize.width)
        (outOffsetPtr + 3).pointee = UInt32(outSize.height)
        
        calculate()
    }
}

class ImageProcessorSplit : ImageProcessor
{
    public override func processCurrentInTexture(_ side: ImageProcessor.Side) {
        guard let maskPtr = maskPtr else { return }
        (maskPtr + 0).pointee = 1.0
        (maskPtr + 1).pointee = 1.0
        (maskPtr + 2).pointee = 1.0
        
        let pieceWidth = outSize.width / 2
        
        guard let outOffsetPtr = outOffsetPtr else { return }
        if (side == .LEFT) {
            (outOffsetPtr + 0).pointee = 0
            (outOffsetPtr + 1).pointee = 0
            (outOffsetPtr + 2).pointee = UInt32(pieceWidth)
            (outOffsetPtr + 3).pointee = UInt32(outSize.height)
        } else {
            (outOffsetPtr + 0).pointee = UInt32(pieceWidth)
            (outOffsetPtr + 1).pointee = 0
            (outOffsetPtr + 2).pointee = UInt32(pieceWidth)
            (outOffsetPtr + 3).pointee = UInt32(outSize.height)
        }
        
        calculate()
    }
}

class ImageProcessorSingle : ImageProcessor
{
    override init(size: ImageUtils.Size) {
        super.init(size: size)
        
        guard let maskPtr = maskPtr else { return }
        (maskPtr + 0).pointee = 1.0
        (maskPtr + 1).pointee = 1.0
        (maskPtr + 2).pointee = 1.0
        
        guard let outOffsetPtr = outOffsetPtr else { return }
        (outOffsetPtr + 0).pointee = 0
        (outOffsetPtr + 1).pointee = 0
        (outOffsetPtr + 2).pointee = UInt32(outSize.width)
        (outOffsetPtr + 3).pointee = UInt32(outSize.height)
    }
    
    public override func processCurrentInTexture(_ side: ImageProcessor.Side)
    {
        guard side == .RIGHT else {
            print("Left side only is used on ImageProcessorSingle")
            return
        }
        
        calculate()
    }
}

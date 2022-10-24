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
import ImageIO
import MobileCoreServices

class ImageProcessor {
    private(set) var _inTexture:MTLTexture?
    private(set) var _midTexture:MTLTexture?
    private(set) var _outTexture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var _device:MTLDevice?
    private var cropState:MTLComputePipelineState?
    private var maskState:MTLComputePipelineState?
    private var clearState:MTLComputePipelineState?
    
    private var offsetBuf:MTLBuffer?
    private var offsetPtr:UnsafeMutablePointer<Int32>?
    
    var rotation:Float = 0.0
    var outSize = ImageUtils.Size(width: 0, height: 0)
    private var maskBuf:MTLBuffer?
    var maskPtr:UnsafeMutablePointer<Float32>?
    private var outOffsetBuf:MTLBuffer?
    var outOffsetPtr:UnsafeMutablePointer<UInt32>?
    private var outTransformBuf:MTLBuffer?
    var outTransformPtr:UnsafeMutablePointer<Float32>?
    
    init(outSize:ImageUtils.Size)
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
        guard let clearfcn = library.makeFunction(name: "clear") else { return }
        do {
            cropState = try device.makeComputePipelineState(function: cropfcn)
            maskState = try device.makeComputePipelineState(function: maskfcn)
            clearState = try device.makeComputePipelineState(function: clearfcn)
        }
        catch {
            return
        }
        
        createOutTexture(outSize)
        
        let intsz = MemoryLayout<Int32>.size
        offsetBuf = device.makeBuffer(length: 3 * intsz, options: .storageModeShared)
        offsetPtr = offsetBuf?.contents().bindMemory(to: Int32.self, capacity: 3)
        
        let floatsz = MemoryLayout<Float32>.size
        maskBuf = device.makeBuffer(length: 3 * floatsz, options: .storageModeShared)
        maskPtr = maskBuf?.contents().bindMemory(to: Float32.self, capacity: 3)
        
        let uintsz = MemoryLayout<UInt32>.size
        outOffsetBuf = device.makeBuffer(length: 4 * uintsz, options: .storageModeShared)
        outOffsetPtr = outOffsetBuf?.contents().bindMemory(to: UInt32.self, capacity: 4)
        
        //3x3 matrix takes up 4 floats per row
        outTransformBuf = device.makeBuffer(length: 12 * floatsz, options: .storageModeShared)
        outTransformPtr = outTransformBuf?.contents().bindMemory(to: Float32.self, capacity: 12)
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
        setPixels(pixels: pixels, rotation: 0.0, offset: CGPoint(x: 0, y: 0))
    }
    
    func setPixels(pixels:CVImageBuffer, rotation:Float, offset:CGPoint) {
        let w = CVPixelBufferGetWidth(pixels)
        let h = CVPixelBufferGetHeight(pixels)
        let margins = ImageUtils.findMargins(size: ImageUtils.Size(width: w, height: h), zoom: 1.0, offset:offset)
        
        setPixels(pixels: pixels, margins: margins, rotation: rotation, offset: offset)
    }
    
    func setPixels(pixels:CVImageBuffer, margins:ImageUtils.Margin, rotation:Float, offset:CGPoint)
    {        
        createInTexture(pixels)
        self.rotation = rotation
        
        let w = CVPixelBufferGetWidth(pixels) - margins.left - margins.right
        let h = CVPixelBufferGetHeight(pixels) - margins.top - margins.bottom
        createMidTexture(size: ImageUtils.Size(width: w, height: h))
        
        guard let offsetPtr = offsetPtr else { return }
        offsetPtr.pointee = Int32(margins.left + Int(offset.x))
        (offsetPtr + 1).pointee = Int32(margins.top + Int(offset.y))
        (offsetPtr + 2).pointee = 0
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
    
    func clear() {
        guard let device = _device,
              let clearState = clearState,
              let outTexture = _outTexture
        else { return }
        
        guard let queue = device.makeCommandQueue(),
              let cmdBuf = queue.makeCommandBuffer(),
              let encoder = cmdBuf.makeComputeCommandEncoder()
        else { return }
        
        encoder.setComputePipelineState(clearState)
        encoder.setTexture(outTexture, index: 0)
        
        let gridSize = MTLSizeMake(outTexture.width, outTexture.height, 1)
        let groupSize = MTLSizeMake(clearState.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.endEncoding()
        cmdBuf.commit()
        cmdBuf.waitUntilCompleted()
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
        setOutTransform()
        encoder.setBuffer(outTransformBuf, offset: 0, index: 2)
        
        gridSize = MTLSizeMake(outTexture.width, outTexture.height, 1)
        groupSize = MTLSizeMake(cropState.maxTotalThreadsPerThreadgroup, 1, 1)
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
    
    public func getFinalImageData() -> Data? {
        guard let img = getOutput(),
              let cs = CGColorSpace(name: CGColorSpace.sRGB)
        else { return nil }
        
        let gammaFilter = GammaFilter(value: 2.2)
        guard let gammaImg = gammaFilter.update(img) else { return nil }
        
        let context = CIContext()
        let jpegData = context.jpegRepresentation(of: gammaImg, colorSpace: cs, options: [:])
        return jpegData
    }
    
    private func setOutTransform() {
        guard let transformPtr = outTransformPtr else { return }
        var mtlTransform = Matrix2D()
        
        //plots the arc of a circle traced by a square of width 2 centered on 0,0
        let rad:Float = sqrt(2.0 * 1.0) //the circle's radius is sqrt(2 * w ^ 2) - from c^2 = a^2 + b^2
        let fortyFive = Float.pi / 4.0
        let ninety = Float.pi / 2.0
        let rot45 = (rotation - fortyFive).remainder(dividingBy: ninety) //constrain the input rotation to -45 to 45 degrees
        let x = sin(rot45) * rad // find the x coordinate from the input rotation
        let ysq = rad * rad - x * x //find the y coordinate of the arc equation sqrt(radius^2 - x^2)
        let zoom = sqrt(ysq) //zoom should be a number between 1.0 and the radius 1.4
        //print ("rot \(x) zoom \(zoom)")
        mtlTransform = mtlTransform.multiply(Matrix2D(scale: 1.0 / zoom))
        
        var rot:Float = -Float.pi / 2.0
        rot -= rotation
        mtlTransform = mtlTransform.multiply(Matrix2D(rotation: rot))
        //print ("debug rot \(rot)")
        
        for row in 0 ..< 3 {
            for col in 0 ..< 4 {
                let srcidx = row * 3 + col
                let destidx = row * 4 + col
                
                (transformPtr + destidx).pointee = (col == 3) ? 0 : mtlTransform.m[srcidx]
            }
        }
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
    override init(outSize: ImageUtils.Size) {
        super.init(outSize: outSize)
        
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
        if (side == .LEFT) {
            return
        }
        
        calculate()
    }
}

class ImageProcessorAnimatedGif : ImageProcessor
{
    let leftFrame:ImageProcessorSingle?
    let rightFrame:ImageProcessorSingle?
    
    let frameDelay:Double
    
    init(outSize: ImageUtils.Size, frameDelay: Double) {
        self.frameDelay = frameDelay
        
        leftFrame = ImageProcessorSingle(outSize: outSize)
        rightFrame = ImageProcessorSingle(outSize: outSize)
        
        super.init(outSize: outSize)
    }
    
    private var currentBuffer:CVImageBuffer?
    private var currentMargins:ImageUtils.Margin?
    private var currentRotation:Float = 0
    private var currentOffset:CGPoint?
    
    override func setPixels(pixels:CVImageBuffer, margins:ImageUtils.Margin, rotation:Float, offset:CGPoint)
    {
        currentBuffer = pixels
        currentMargins = margins
        currentRotation = rotation
        currentOffset = offset
    }
    
    override func processCurrentInTexture(_ side: ImageProcessor.Side) {
        guard let proc = (side == .LEFT) ? leftFrame : rightFrame,
              let buf = currentBuffer,
              let margins = currentMargins,
              let offset = currentOffset
        else { return }
        
        proc.setPixels(pixels: buf, margins: margins, rotation: currentRotation, offset: offset)
        proc.processCurrentInTexture(.RIGHT)
    }
    
    private func getCGImage(_ proc:ImageProcessor?) -> CGImage? {
        guard let img = proc?.getOutput() else { return nil }
        
        let gammaFilter = GammaFilter(value: 2.2)
        guard let gammaImg = gammaFilter.update(img) else { return  nil}
        
        let ctx = CIContext()
        let sz = gammaImg.extent
        let ret = ctx.createCGImage(gammaImg, from: sz)
        
        return ret
    }
    
    public override func getFinalImageData() -> Data? {
        guard let leftImg = getCGImage(leftFrame),
              let rightImg = getCGImage(rightFrame),
              let ptr = CFDataCreateMutable(nil, 0)
        else { return nil }
        
        let fileProperties = [
            (kCGImagePropertyGIFDictionary as String): [(kCGImagePropertyGIFLoopCount as String): 0]
        ]
        
        let destinationGIF = CGImageDestinationCreateWithData(ptr, kUTTypeGIF, 2, nil)!
        CGImageDestinationSetProperties(destinationGIF, fileProperties as CFDictionary?);

        // This dictionary controls the delay between frames
        // If you don't specify this, CGImage will apply a default delay
        let frameProperties = [
            (kCGImagePropertyGIFDictionary as String): [(kCGImagePropertyGIFDelayTime as String): frameDelay]
        ]
        CGImageDestinationAddImage(destinationGIF, leftImg, frameProperties as CFDictionary?)
        CGImageDestinationAddImage(destinationGIF, rightImg, frameProperties as CFDictionary?)

        // Write the GIF file to disk
        CGImageDestinationFinalize(destinationGIF)
        let ret = ptr as NSData as Data //the double cast is necessary for some reason
        return ret
    }
}

class ImageProcessorFake : ImageProcessor {
    
    override func clear() {}
    
    override func setPixels(pixels:CVImageBuffer, margins:ImageUtils.Margin, rotation:Float, offset:CGPoint)
    {
    }
    
    override public func getOutput() -> CIImage? {
        let ret = CIImage(color: .gray)
        
        return ret
    }
}

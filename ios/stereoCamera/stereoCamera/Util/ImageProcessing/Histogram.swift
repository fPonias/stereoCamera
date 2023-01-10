//
//  Histogram.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/21/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import MetalKit
import CoreImage

class Histogram
{
    private var _texture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var _device:MTLDevice?
    private var histState:MTLComputePipelineState?
    private var marginBuf:MTLBuffer?
    private var resultBuf:MTLBuffer?
    private var histogramPtr:UnsafeMutablePointer<Int32>?
    private let SZ = 256
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
            histState = try device.makeComputePipelineState(function: fcn)
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
    
    public struct TextureMargin {
        var left:Int
        var top:Int
        var right:Int
        var bottom:Int
    }
    
    private var margins = TextureMargin(left: 0, top: 0, right: 0, bottom: 0)
    
    func getMargins() -> TextureMargin {
        return margins
    }
    
    func setMargins(_ margins:TextureMargin) {
        self.margins = margins
        guard let ptr = marginBuf?.contents() else { return }
        
        let sizeof = MemoryLayout<Int32>.size
        ptr.storeBytes(of: Int32(margins.left), toByteOffset: 0, as: Int32.self)
        ptr.storeBytes(of: Int32(margins.top), toByteOffset: sizeof, as: Int32.self)
        ptr.storeBytes(of: Int32(margins.right), toByteOffset: sizeof * 2, as: Int32.self)
        ptr.storeBytes(of: Int32(margins.bottom), toByteOffset: sizeof * 3, as: Int32.self)
    }
    
    var texture:CVImageBuffer?
    
    func setPixels(pixels:CVImageBuffer)
    {
        texture = ImageUtils.copyBuffer(base: pixels)
        
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
    
    func setPixels(mtlPixels:MTLTexture)
    {
        width = mtlPixels.width
        height = mtlPixels.height
        _texture = mtlPixels
    }
    
    public func hasTexture() -> Bool {
        return _texture != nil
    }
    
    
    
    func setPixelZoom(_ zoom:Int) {
        let margin:TextureMargin
        if (width > height) {
            let trim = (width - height) / 2
            margin = TextureMargin(left: zoom + trim, top: zoom, right: zoom + trim, bottom: zoom)
        } else {
            let trim = (height - width) / 2
            margin = TextureMargin(left: zoom, top: zoom + trim, right: zoom, bottom: zoom + trim)
        }
        
        setMargins(margin)
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
              let histState = histState
        else { return nil }
            
        histogramPtr.assign(repeating: 0, count: SZ)
                
        guard let queue = device.makeCommandQueue(),
              let cmdBuf = queue.makeCommandBuffer(),
              let encoder = cmdBuf.makeComputeCommandEncoder()
        else { return nil }
        
        encoder.setComputePipelineState(histState)
        encoder.setTexture(_texture, index: 0)
        
        encoder.setBuffer(resultBuf, offset: 0, index: 0)
        encoder.setBuffer(marginBuf, offset: 0, index: 1)
        
        let gridSize = MTLSizeMake(width, height, 1)
        let groupSize = MTLSizeMake(histState.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.endEncoding()
        cmdBuf.commit()
        cmdBuf.waitUntilCompleted()
        
        return Array(UnsafeBufferPointer(start: histogramPtr, count: SZ))
    }
    
    func average() -> Float {
        guard let arr = calculate() else { return 0 }
        
        let sz = Float(width - margins.left - margins.right) * Float(height - margins.top - margins.bottom)
        
        var ret:Float = 0.0
        var c = 0
        
        for i in 0 ..< arr.count {
            c += Int(arr[i])
            ret += Float(Int(arr[i]) * i) / sz
        }
        
        return ret
    }
    
    func smooth(data:[Int32]) -> [Int32] {
        let sz = data.count
        
        if (sz <= 5) { return data }
        
        let weights = [1.0/16.0, 3.0/16.0, 0.5, 3.0/16.0, 1.0/16.0]
        var ret:[Int32] = Array(repeating: 0, count: sz)
        for i in 0 ..< sz {
            var val = 0.0
            for j in 0 ..< weights.count {
                let idx = min(max(i + j - 2, 0), sz - 1)
                val += Double(data[idx]) * weights[j]
            }
            ret[i] = Int32(val)
        }
        
        return ret
    }
    
    func derivative(data:[Int32]) -> [Int32] {
        let sz = data.count
        var ret:[Int32] = Array(repeating: 0, count: sz - 1)
        for i in 1 ..< sz {
            ret[i - 1] = data[i] - data[i - 1]
        }
        
        return ret
    }
    
    struct ContrastParams {
        var min:Double
        var max:Double
        var a:Double
        var b:Double
    }
    
    private func getPercentIndex(histogram:[Int32], percent:Double) -> Double {
        guard(percent >= 0 && percent <= 1.0) else { return 0 }
        
        var count:Int32 = 0
        for value in histogram {
            count += value
        }
        
        var ret = 0.0
        var total:Int32 = 0
        var percentSum = 0.0
        let countf = Double(count)
        let p = Int32(countf * percent)
        let incPercent = 1.0 / Double(histogram.count)
        
        for i in 0 ..< histogram.count {
            let inc = histogram[i]
            total += inc
            percentSum = Double(total) / countf
            
            if percentSum > percent {
                let rem = total - p
                let subPercent = Double(rem) / Double(inc)
                ret = Double(i - 1) / Double(histogram.count) + incPercent * subPercent
                break
            }
        }
        
        return ret
    }
    
    private func getBottomIndex(histogram:[Int32], threshold: Int) -> Double {
        for i in 0 ..< histogram.count - 1 {
            if histogram[i] > threshold && histogram[i] < histogram[i + 1] {
                return Double(i) / Double(histogram.count)
            }
        }
        
        return 0
    }
    
    private func getTopIndex(histogram:[Int32], threshold: Int) -> Double {
        for i in 0 ..< histogram.count - 1 {
            let idx = histogram.count - i - 1
            if histogram[idx] > threshold && histogram[idx] < histogram[idx - 1] {
                return Double(idx) / Double(histogram.count)
            }
        }
        
        return 1.0
    }
    
    func autoContrastParams() -> ContrastParams {
        var ret = ContrastParams(min: 0.0, max: 1.0, a:0.0, b:0.0)
        guard let hist = calculate() else { return ret }
        //ret.min = getPercentIndex(histogram: hist, percent: 0.05)
        //ret.max = getPercentIndex(histogram: hist, percent: 0.95)
        
        let dampening = 0.5
        ret.min = getBottomIndex(histogram: hist, threshold: 1) * dampening
        let top = getTopIndex(histogram: hist, threshold: 1)
        ret.max = (1.0 - top) * dampening + top
        
        //a*p5+b=0 and a*p95+b=255
        let p_div = ret.min / ret.max
        ret.b = -1.0 * p_div / (1.0 - p_div)
        ret.a = (1.0 - ret.b) / ret.max
        
        //let checkOne = ret.a * ret.min + ret.b  //should be close to 0
        //let checkTwo = ret.a * ret.max + ret.b  //should be close to 1
        
        return ret
    }
}

public class ReducedHistogram {
    private var _texture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var _device:MTLDevice?
    private var histState:MTLComputePipelineState?
    private var marginBuf:MTLBuffer?
    private var resultBuf:MTLBuffer?
    private var histogramPtr:UnsafeMutablePointer<Int32>?
    private var marginPtr:UnsafeMutablePointer<Float32>?
    private let SZ = 256
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
        guard let fcn = library.makeFunction(name: "histogramReduced") else { return }
        do {
            histState = try device.makeComputePipelineState(function: fcn)
        }
        catch {
            return
        }
        
        let sizeof = MemoryLayout<Int32>.size
        resultBuf = device.makeBuffer(length: SZ * sizeof, options: .storageModeShared)
        histogramPtr = resultBuf?.contents().bindMemory(to: Int32.self, capacity: SZ)
        
        marginBuf = device.makeBuffer(length: 6 * sizeof, options: .storageModeShared)
        marginPtr = marginBuf?.contents().bindMemory(to: Float32.self, capacity: 6)
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
    
    func setZoom(_ zoom:Float, offset:CGPoint)
    {
        guard let marginPtr = marginPtr else { return }
        let margins = ImageUtils.findFloatMargins(size: ImageUtils.Size(width: width, height: height), zoom: zoom, offset: offset)
        ImageUtils.marginFloatToMtlArr(margins: margins, ptr: marginPtr)
    }
    
    func calculate() -> [Int32]? {
        guard let device = _device,
              let resultBuf = resultBuf,
              let histogramPtr = histogramPtr,
              let histState = histState
        else { return nil }
            
        histogramPtr.assign(repeating: 0, count: SZ)
                
        guard let queue = device.makeCommandQueue(),
              let cmdBuf = queue.makeCommandBuffer(),
              let encoder = cmdBuf.makeComputeCommandEncoder()
        else { return nil }
        
        encoder.setComputePipelineState(histState)
        encoder.setTexture(_texture, index: 0)
        
        encoder.setBuffer(resultBuf, offset: 0, index: 0)
        encoder.setBuffer(marginBuf, offset: 0, index: 1)
        
        let gridSize = MTLSizeMake(width, height, 1)
        let groupSize = MTLSizeMake(histState.maxTotalThreadsPerThreadgroup, 1, 1)
        encoder.dispatchThreads(gridSize, threadsPerThreadgroup: groupSize)
        
        encoder.endEncoding()
        cmdBuf.commit()
        cmdBuf.waitUntilCompleted()
        
        return Array(UnsafeBufferPointer(start: histogramPtr, count: SZ))
    }
}

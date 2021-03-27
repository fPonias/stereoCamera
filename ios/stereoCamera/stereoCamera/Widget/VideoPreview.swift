//
//  VideoPreview.swift
//  stereoCamera
//
//  Created by Cody Munger on 3/14/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import UIKit
import AVFoundation
import MetalKit
import MetalPerformanceShaders
import Photos
import CoreMedia

public protocol VideoPreviewDelegate
{
    func firstTextureReceived(_ preview: VideoPreview, image:CVImageBuffer, orientation:ImageUtils.CameraOrientation)
}

extension Matrix
{
    func fillMemory(_ ptr:UnsafeMutablePointer<Float32>)
    {
        for i in 0 ..< 16 {
            (ptr + i).pointee = Float32(m[i])
        }
    }
}

public class VideoPreview : MTKView, AVCaptureVideoDataOutputSampleBufferDelegate, AVCapturePhotoCaptureDelegate, MTKViewDelegate {
    
    public var previewDelegate: VideoPreviewDelegate?
    
    public override init(frame frameRect: CGRect, device: MTLDevice?)
    {
        super.init(frame: frameRect, device: device)
    }

    public required init(coder: NSCoder)
    {
        super.init(coder: coder)
    }

    deinit {
        UIDevice.current.endGeneratingDeviceOrientationNotifications()
        NotificationCenter.default.removeObserver(self)
        
    }
    
    private var _zoom:Float = 1.0
    var zoom:Float
    {
        get {return _zoom}
        set
        {
            _zoom = newValue
            updateTransform()
        }
    }
    
    public func initialize() {
        initializeMetal()
        initializeListeners()
    }
    
    private func initializeMetal(){
        if (device == nil) {
            device = MTLCreateSystemDefaultDevice()
        }
        
        guard let device = device,
              CVMetalTextureCacheCreate(kCFAllocatorDefault, nil, device, nil, &_textureCache) == kCVReturnSuccess
        else { return }
        
        delegate = self
        framebufferOnly = true
        colorPixelFormat = .bgra8Unorm
        contentScaleFactor = UIScreen.main.scale
        autoresizingMask = [.flexibleWidth, .flexibleHeight]
        
        let sz = MemoryLayout<Float32>.size
        transformBuffer = device.makeBuffer(length: sz * 16, options: .storageModeShared)
        transformPtr = transformBuffer?.contents().bindMemory(to: Float32.self, capacity: 16)
        
        setupPixels()
        
        initializeRenderPipelineState()
    }
    
    public func initializeListeners() {
        NotificationCenter.default.addObserver(self, selector: #selector(onOrientation(_:)), name: .UIDeviceOrientationDidChange, object: nil)
        
        let device = UIDevice.current
        device.beginGeneratingDeviceOrientationNotifications()
        let orient = UIApplication.shared.statusBarOrientation
        orientation = ImageUtils.imageOrientationToProcOrientation(UIApplication.shared.statusBarOrientation, false)
        updateTransform()
    }
    
    @objc func onOrientation(_ notification: Notification) {
        orientation =  ImageUtils.deviceOrientationToProcOrientation(UIDevice.current.orientation, false)
        updateTransform()
    }
    
    func captureOutput(_ output: AVCaptureOutput, didDrop sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
    }
    
    private var _texture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var renderPipelineState:MTLRenderPipelineState?
    private var transformBuffer:MTLBuffer?
    private var transformPtr:UnsafeMutablePointer<Float32>?
    private var width:Int = 0
    private var height:Int = 0
    private var hasNewTexture:Bool = false
    private var sentFirstTexture:Bool = false
    private var firstTextureReceived:TimeInterval = 0
    
    func captureOutput(captureOutput: AVCaptureOutput!, didOutputSampleBuffer sampleBuffer:CMSampleBuffer!, fromConnection connection: AVCaptureConnection!)
    {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        let w = CVPixelBufferGetWidth(pixelBuffer)
        let h = CVPixelBufferGetHeight(pixelBuffer)
        
        if (previewDelegate != nil && !sentFirstTexture)
        {
            if (firstTextureReceived == 0) {
                firstTextureReceived = NSDate().timeIntervalSince1970
            }
            else {
                let now = NSDate().timeIntervalSince1970
                if (now - firstTextureReceived >= 1.0) {
                    sentFirstTexture = true
                    previewDelegate?.firstTextureReceived(self, image: pixelBuffer, orientation: orientation ?? .DEG_0)
                }
            }
        }
        
        if (nextFrameCallback != nil) {
            nextFrameCallback?(pixelBuffer, margins, orientation ?? .DEG_0)
            nextFrameCallback = nil
        }
        
        
        if (w != width || h != height)
        {
            width = w
            height = h
            updateTransform()
        }
        
        guard let unwrappedCache = _textureCache else { return }
        var texture:CVMetalTexture?
        let status = CVMetalTextureCacheCreateTextureFromImage(kCFAllocatorDefault, unwrappedCache, pixelBuffer, nil, MTLPixelFormat.bgra8Unorm, width, height, 0, &texture)
        
        guard (status == kCVReturnSuccess),
              let unwrappedTexture = texture
        else { return }
        _texture = CVMetalTextureGetTexture(unwrappedTexture)
        hasNewTexture = true
  
        draw()
    }
    
    private var nextFrameCallback:((CVImageBuffer, ImageUtils.Margin, ImageUtils.CameraOrientation) -> Void)?
    
    func getNextFrame(callback: @escaping (CVImageBuffer, ImageUtils.Margin, ImageUtils.CameraOrientation) -> Void) {
        guard nextFrameCallback == nil else { return }
        nextFrameCallback = callback
    }
    
    public func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        
    }
    
    private struct Vertex {
        var x:Float32
        var y:Float32
        var depth:Float32
        var w:Float32
    }
    
    private struct TextureCoord {
        var x:Float32
        var y:Float32
    }
    
    private class VertexTextureMap {
        var vertex = Vertex(x:0.0, y:0.0, depth:0.0, w:1.0)
        var coord = TextureCoord(x:0.0, y:0.0)
        
        func toArr() -> [Float32]
        {
            return [vertex.x, vertex.y, vertex.depth, vertex.w, coord.x, coord.y, 0.0, 0.0]
        }
    }
    
    private var pixels = [Float32]()
    private var pixelData = [VertexTextureMap]()
    
    func setupPixels() {
        let v1 = VertexTextureMap()
        v1.vertex.x = -1.0; v1.vertex.y = -1.0
        v1.coord.x = 0.0; v1.coord.y = 1.0
        pixelData.append(v1)
        
        let v2 = VertexTextureMap()
        v2.vertex.x = 1.0; v2.vertex.y = -1.0
        v2.coord.x = 1.0; v2.coord.y = 1.0
        pixelData.append(v2)
        
        let v3 = VertexTextureMap()
        v3.vertex.x = -1.0; v3.vertex.y = 1.0
        v3.coord.x = 0.0; v3.coord.y = 0.0
        pixelData.append(v3)
        
        let v4 = VertexTextureMap()
        v4.vertex.x = 1.0; v4.vertex.y = 1.0
        v4.coord.x = 1.0; v4.coord.y = 0.0
        pixelData.append(v4)
    }
    
    private var margins = ImageUtils.Margin(left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0)
    private let mtlTransform = Matrix()
    
    private var orientation:ImageUtils.CameraOrientation?
    
    func updateTransform()
    {
        let orientation = self.orientation ?? .DEG_0
        let rotation = ImageUtils.orientationToRadians(orientation)
        
        margins = ImageUtils.findMargins(size: ImageUtils.Size(width: width, height: height), zoom: _zoom)
        let floatMargin = ImageUtils.findFloatMargins(size: ImageUtils.Size(width: width, height: height), zoom: _zoom)
    
        pixelData[0].coord.x = floatMargin.left
        pixelData[0].coord.y = floatMargin.bottom
        pixelData[1].coord.x = floatMargin.right
        pixelData[1].coord.y = floatMargin.bottom
        pixelData[2].coord.x = floatMargin.left
        pixelData[2].coord.y = floatMargin.top
        pixelData[3].coord.x = floatMargin.right
        pixelData[3].coord.y = floatMargin.top
            
        pixels = [Float32]()
        for pd in pixelData {
            pixels.append(contentsOf: pd.toArr())
        }
        
        mtlTransform.identity()
        mtlTransform.rotate(Float3(x: 0, y: 0, z: Float(rotation)))
        guard let ptr = transformPtr else { return }
        mtlTransform.fillMemory(ptr)
    }
    
    public func draw(in view: MTKView) {
        guard let texture = _texture,
              let device = device,
              hasNewTexture
        else { return }
        
        hasNewTexture = false
        
        let commandBuffer = device.makeCommandQueue()?.makeCommandBuffer()
        guard let pixelBuf = device.makeBuffer(length: pixels.count, options: .storageModeShared) else { return }
        let ptr = pixelBuf.contents().bindMemory(to: Float32.self, capacity: pixels.count)
        ptr.assign(from: pixels, count: pixels.count)
        
        
        guard let currentRenderPassDesc = view.currentRenderPassDescriptor,
              let currentDrawable = view.currentDrawable,
              let renderPipelineState = renderPipelineState
        else { return }
        
        let encoder = commandBuffer?.makeRenderCommandEncoder(descriptor: currentRenderPassDesc)
        encoder?.pushDebugGroup("RenderFrame")
        encoder?.setRenderPipelineState(renderPipelineState)
        encoder?.setVertexBuffer(pixelBuf, offset: 0, index: 0)
        encoder?.setVertexBuffer(transformBuffer, offset: 0, index: 1)
        encoder?.setFragmentTexture(texture, index: 0)
        encoder?.drawPrimitives(type: .triangleStrip, vertexStart: 0, vertexCount: 4, instanceCount: 1)
        encoder?.popDebugGroup()
        encoder?.endEncoding()
        
        commandBuffer?.present(currentDrawable)
        commandBuffer?.commit()
    }
    
    private func initializeRenderPipelineState() {
        guard let device = device,
              let library = device.makeDefaultLibrary()
        else { return }
        
        let pipelineDescriptor = MTLRenderPipelineDescriptor()
        pipelineDescriptor.sampleCount = 1
        pipelineDescriptor.colorAttachments[0].pixelFormat = .bgra8Unorm
        pipelineDescriptor.depthAttachmentPixelFormat = .invalid
        pipelineDescriptor.vertexFunction = library.makeFunction(name: "mapTexture")
        pipelineDescriptor.fragmentFunction = library.makeFunction(name: "displayTexture")
        
        do {
            try renderPipelineState = device.makeRenderPipelineState(descriptor: pipelineDescriptor)
        }
        catch {
            assertionFailure("Failed to create a render state pipeline")
        }
    }
}

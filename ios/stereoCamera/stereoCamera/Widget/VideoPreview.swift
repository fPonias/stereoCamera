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
            //print ("manual zoom deprecated in favor of session zoom")
            _zoom = newValue
            updateTransform()
        }
    }
    
    private var _offset:CGPoint = CGPoint()
    var offset:CGPoint
    {
        get { return _offset }
        set
        {
            _offset = newValue
            updateTransform()
        }
    }
    
    public func initialize() {
        initializeMetal()
        initializeListeners()
    }
    
    private func initializeMetal(){
        if  (device == nil) {
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
        //orientation = ImageUtils.imageOrientationToProcOrientation(UIApplication.shared.statusBarOrientation, false)
        //updateTransform()
    }
    
    @objc func onOrientation(_ notification: Notification) {
        //orientation =  ImageUtils.deviceOrientationToProcOrientation(UIDevice.current.orientation, false)
        //updateTransform()
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
    
    public func renderBuffer(sampleBuffer: CMSampleBuffer) {
        guard let lPixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        renderBuffer(lPixelBuffer)
    }
    
    public func renderBuffer(_ pixelBuffer:CVImageBuffer) {
        let w = CVPixelBufferGetWidth(pixelBuffer)
        let h = CVPixelBufferGetHeight(pixelBuffer)
        
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
    
    private var processing = false
    
    public func renderTexture(_ texture:MTLTexture?) {
        guard processing == false else { return }
        guard let texture = texture else { return }
        
        processing = true
        
        _texture = texture
        hasNewTexture = true
        
        let w = texture.width
        let h = texture.height
        
        if (w != width || h != height)
        {
            width = w
            height = h
            updateTransform()
        }
        
        draw()
        
        processing = false
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
    
    //var orientation:ImageUtils.CameraOrientation?
    private var _rotation:Float = 0.0
    public var rotation:Float {
        get { return _rotation }
        set(value) {
            _rotation = value
            updateTransform()
        }
    }
    
    func updateTransform()
    {
        //let orientation = self.orientation ?? .DEG_0
        //let rotation = ImageUtils.orientationToRadians(orientation)
        
        let sz = ImageUtils.Size(width: width, height: height)
        margins = ImageUtils.findMargins(size: sz, zoom: _zoom, offset: offset)
        let floatMargin = ImageUtils.findFloatMargins(size: sz, zoom: _zoom, offset: offset)
    
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
        
        var mtlTransform = Matrix()
        
        //plots the arc of a circle traced by a square of width 2 centered on 0,0
        let rad:Float = sqrt(2.0 * 1.0) //the circle's radius is sqrt(2 * w ^ 2) - from c^2 = a^2 + b^2
        let fortyFive = Float.pi / 4.0
        let ninety = Float.pi / 2.0
        let rot45 = (_rotation - fortyFive).remainder(dividingBy: ninety) //constrain the input rotation to -45 to 45 degrees
        let x = sin(rot45) * rad // find the x coordinate from the input rotation
        let ysq = rad * rad - x * x //find the y coordinate of the arc equation sqrt(radius^2 - x^2)
        let zoom = sqrt(ysq) //zoom should be a number between 1.0 and the radius 1.4
        //print ("rot \(x) zoom \(zoom)")
        mtlTransform = mtlTransform.multiply(Matrix(scale: zoom))
        
        var rot:Float = -Float.pi / 2.0
        rot += _rotation
        mtlTransform = mtlTransform.multiply(Matrix(rotation: Float3(x: 0, y: 0, z: rot)))
        //print ("debug rot \(rot)")
        
        
        guard let ptr = transformPtr else { return }
        mtlTransform.fillMemory(ptr)
    }
    
    var okayToDraw = true
    
    public func stopDrawing() {
        okayToDraw = false
    }
    
    public func resumeDrawing() {
        okayToDraw = true
    }
    
    public func draw(in view: MTKView) {
        guard okayToDraw else { return }
        
        guard let texture = _texture,
              let device = device,
              hasNewTexture
        else { return }
        
        hasNewTexture = false
        
        let commandBuffer = device.makeCommandQueue()?.makeCommandBuffer()
        
        guard pixels.count > 0 else { return }
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

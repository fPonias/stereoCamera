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

public class VideoPreviewDouble : MTKView, AVCaptureVideoDataOutputSampleBufferDelegate, AVCapturePhotoCaptureDelegate, MTKViewDelegate {
    
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
            print ("manual zoom deprecated in favor of session zoom")
            //_zoom = newValue
            //updateTransform()
        }
    }
    
    public func initialize() {
        initializeMetal()
        initializeListeners()
    }
    
    func setupProcessor(type:ImageFormat, size:CGSize) {
        let sz = ImageUtils.Size(width: Int(size.width), height: Int(size.height))
        switch(type){
        case .SPLIT: _imageProc = ImageProcessorSplit(outSize: sz)
        case .GREEN_MAGENTA: _imageProc = ImageProcessorGreenMagenta(outSize: sz)
        case .RED_BLUE: _imageProc = ImageProcessorRedCyan(outSize: sz)
        case .ANIMATED: _imageProc = nil
        }
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
    
    private var _imageProc:ImageProcessor?
    private var _texture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var renderPipelineState:MTLRenderPipelineState?
    private var transformBuffer:MTLBuffer?
    private var transformPtr:UnsafeMutablePointer<Float32>?
    private var width:Int = 0
    private var height:Int = 0
    
    private var hasLeftTexture:Bool = false
    private var hasRightTexture:Bool = false
    private var hasNewTexture:Bool = false
    private var sentFirstTexture:Bool = false
    private var firstTextureReceived:TimeInterval = 0
    
    public var frameListener:(MTLTexture) -> Void = {_ in }
    
    func renderBuffer(sampleBuffer: CMSampleBuffer, side:ImageProcessor.Side) {
        guard let lPixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
        renderBuffer(lPixelBuffer, side: side, rotation: rotation)
    }
    
    func renderBuffer(_ pixelBuffer:CVImageBuffer, side:ImageProcessor.Side, rotation:Float) {
        guard hasNewTexture == false,
              (side == .LEFT && !hasLeftTexture) || (side == .RIGHT && !hasRightTexture),
              let imageProc = _imageProc
        else { return }
        
        setProcessorImage(pixelBuffer, side: side, rotation: rotation)
        
        DispatchQueue.main.sync {
            if (side == .LEFT) {
                hasLeftTexture = true
            } else {
                hasRightTexture = true
            }
            
            guard hasLeftTexture && hasRightTexture else { return }
            
            hasLeftTexture = false
            hasRightTexture = false
            
            _texture = imageProc._outTexture
            hasNewTexture = true
            
            if let texture = _texture {
                frameListener(texture)
            }
      
            draw()
        }
    }
    
    private func setProcessorImage(_ pixelBuffer:CVImageBuffer, side:ImageProcessor.Side, rotation:Float)
    {
        guard let imageProc = _imageProc else { return }
        
        if (!hasLeftTexture && !hasRightTexture) {
            imageProc.clear()
        }
        
        let w = CVPixelBufferGetWidth(pixelBuffer) - margins.left - margins.right
        let h = CVPixelBufferGetHeight(pixelBuffer) - margins.top - margins.bottom
        let margins = ImageUtils.findMargins(size: ImageUtils.Size(width: w, height: h), zoom: 1.0)
        imageProc.setPixels(pixels: pixelBuffer, margins: margins, rotation: rotation)
        imageProc.processCurrentInTexture(side)
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
    
    private var offset:Float = 0.0
    
    func updateTransform()
    {
        pixelData[0].coord.x = 0
        pixelData[0].coord.y = 0
        pixelData[1].coord.x = 1
        pixelData[1].coord.y = 0
        pixelData[2].coord.x = 0
        pixelData[2].coord.y = 1
        pixelData[3].coord.x = 1
        pixelData[3].coord.y = 1
 
            
        pixels = [Float32]()
        for pd in pixelData {
            pixels.append(contentsOf: pd.toArr())
        }
        
        let transform = Matrix()
        guard let ptr = transformPtr else { return }
        transform.fillMemory(ptr)
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
        currentRenderPassDesc.colorAttachments[0].clearColor = MTLClearColorMake(0, 0, 0, 1)
        currentRenderPassDesc.colorAttachments[0].loadAction = MTLLoadAction.clear
        encoder?.pushDebugGroup("RenderFrameDouble")
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

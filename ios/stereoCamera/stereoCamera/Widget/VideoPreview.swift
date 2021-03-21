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

class VideoPreview : MTKView, AVCaptureVideoDataOutputSampleBufferDelegate, AVCapturePhotoCaptureDelegate, MTKViewDelegate {
    
    
    public override init(frame frameRect: CGRect, device: MTLDevice?)
    {
        super.init(frame: frameRect, device: device)
        guard let _device = device else { return }
    }

    public required init(coder: NSCoder)
    {
        super.init(coder: coder)
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
    
    private var previewTransform:CGAffineTransform = CGAffineTransform()
    
    public enum CameraOriention:CGFloat
    {
        case DEG_0 = 0.0,
        DEG_90 = 90.0,
        DEG_180 = 180.0,
        DEG_270 = 270.0
    }
    
    public static func orientationToRadians(_ orientation:CameraOriention) -> CGFloat
    {
        switch(orientation)
        {
        case .DEG_0:
            return 0.0
        case .DEG_90:
            return CGFloat(Double.pi)
        case .DEG_180:
            return CGFloat(Double.pi / 2.0)
        case .DEG_270:
            return CGFloat(3 * Double.pi / 2.0)
        }
    }
    
    public static func orientationToByte(_ orientation:CameraOriention) -> UInt8
    {
        switch(orientation)
        {
        case .DEG_0:
            return 0
        case .DEG_90:
            return 1
        case .DEG_180:
            return 2
        case .DEG_270:
            return 3
        }
    }
    
    public static func orientationFromByte(_ b:UInt8) -> CameraOriention
    {
        switch(b)
        {
        case 0:
            return .DEG_0
        case 1:
            return .DEG_90
        case 2:
            return .DEG_180
        case 3:
            return .DEG_270
        default:
            return .DEG_0
        }
    }
    
    open func isFacing() -> Bool
    {
        return false
    }
    
    public func getScreenOrientation() -> CameraOriention
    {
        let facing = isFacing()
        let orient = UIDevice.current.orientation
        switch(orient)
        {
        case .portrait:
            return .DEG_270
        case .portraitUpsideDown:
            return .DEG_90
        case .landscapeRight:
            return (facing) ? .DEG_0 : .DEG_90 //why is this offset by 90 degrees?  I'm so confused.
        case .landscapeLeft:
            return (facing) ? .DEG_90 : .DEG_0
        default:
            return .DEG_0
        }
    }
    
    public func getOrientation() -> CameraOriention
    {
        let facing = isFacing()
        let orient = UIDevice.current.orientation
        switch(orient)
        {
        case .portrait:
            return .DEG_90
        case .portraitUpsideDown:
            return .DEG_270
        case .landscapeRight:
            return (facing) ? .DEG_180 : .DEG_0
        case .landscapeLeft:
            return (facing) ? .DEG_0 : .DEG_180
        default:
            return .DEG_0
        }
    }
    
    private let cameraVideoDataOutput = AVCaptureVideoDataOutput()
    private let dataOutputQueue = DispatchQueue(label: "data output queue")
    private var cameraVideoDataOutputConnection: AVCaptureConnection?
    
    @available(iOS 13.0, *)
    func configureDualCamera(camera:AVCaptureDevice.DeviceType, session:AVCaptureMultiCamSession) -> Bool
    {
        session.beginConfiguration()
        defer {
            session.commitConfiguration()
        }
        
        // Find the back camera
        guard let camera = AVCaptureDevice.default(camera, for: .video, position: .back) else {
            print("Could not find the back camera")
            return false
        }
        
        // Add the front camera input to the session
        let cameraDeviceInput: AVCaptureDeviceInput
        do {
            cameraDeviceInput = try AVCaptureDeviceInput(device: camera)
            
            guard session.canAddInput(cameraDeviceInput) else {
                    print("Could not add front camera device input")
                    return false
            }
            session.addInputWithNoConnections(cameraDeviceInput)
        } catch {
            print("Could not create front camera device input: \(error)")
            return false
        }
        
        // Find the front camera device input's video port
        guard let frontCameraVideoPort = cameraDeviceInput.ports(for: .video,
            sourceDeviceType: camera.deviceType,
            sourceDevicePosition: camera.position).first else {
                print("Could not find the camera device input's video port")
                return false
        }
        
        // Add the front camera video data output
        guard session.canAddOutput(cameraVideoDataOutput) else {
            print("Could not add the front camera video data output")
            return false
        }
        session.addOutputWithNoConnections(cameraVideoDataOutput)
        cameraVideoDataOutput.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        cameraVideoDataOutput.setSampleBufferDelegate(self, queue: dataOutputQueue)
        
        // Connect the front camera device input to the front camera video data output
        cameraVideoDataOutputConnection = AVCaptureConnection(inputPorts: [frontCameraVideoPort], output: cameraVideoDataOutput)
        guard let cameraVideoDataOutputConnection = cameraVideoDataOutputConnection,
              session.canAdd(cameraVideoDataOutputConnection) else {
            print("Could not add a connection to the front camera video data output")
            return false
        }
        session.add(cameraVideoDataOutputConnection)
        cameraVideoDataOutputConnection.videoOrientation = .portrait
        cameraVideoDataOutputConnection.automaticallyAdjustsVideoMirroring = false
        
        return true
    }
    
    public func initializeMetal(){
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
        
        setupPixels()
        
        initializeRenderPipelineState()
    }
    
    func rotateScaleImage(image: CIImage) -> CIImage
    {
        var ret:CIImage = image
        
        let inRect = image.extent
        let margin = (inRect.width - inRect.height) / 2
        let cropped = CGRect(x: margin, y: 0, width: inRect.height, height: inRect.height)
        ret = ret.cropped(to: cropped)
        
        ret = ret.transformed(by: previewTransform)
        
        return ret
    }
    
    func captureOutput(_ output: AVCaptureOutput, didDrop sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
    }
    
    private var _texture:MTLTexture?
    private var _textureCache:CVMetalTextureCache?
    private var renderPipelineState:MTLRenderPipelineState?
    private var width:Int = 0
    private var height:Int = 0
    
    func captureOutput(captureOutput: AVCaptureOutput!, didOutputSampleBuffer sampleBuffer:CMSampleBuffer!, fromConnection connection: AVCaptureConnection!)
    {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
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
  
        draw()
    }
    
    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        
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
    
    func updateTransform()
    {
        //let orientation = getScreenOrientation();
        //let rotation = CameraPreview.orientationToRadians(orientation)
        
        if (width < height)
        {
            let margin = (height - width) / 2
            let frac = Float(margin) / Float(height)
            let fracInv = 1.0 - frac
            
            pixelData[0].coord.x = 0.0; pixelData[0].coord.y = fracInv
            pixelData[1].coord.x = 1.0; pixelData[1].coord.y = fracInv
            pixelData[2].coord.x = 0.0; pixelData[2].coord.y = frac
            pixelData[3].coord.x = 1.0; pixelData[3].coord.y = frac
        }
        else
        {
            let margin = (width - height) / 2
            let frac = Float(margin) / Float(width)
            let fracInv = 1.0 - frac
            
            pixelData[0].coord.x = frac; pixelData[0].coord.y = 1.0
            pixelData[1].coord.x = fracInv; pixelData[1].coord.y = 1.0
            pixelData[2].coord.x = frac; pixelData[2].coord.y = 0.0
            pixelData[3].coord.x = fracInv; pixelData[3].coord.y = 0.0
        }
        
        pixels = [Float32]()
        for pd in pixelData {
            pixels.append(contentsOf: pd.toArr())
        }
    }
    
    func draw(in view: MTKView) {
        guard let texture = _texture,
              let device = device
        else { return }
        
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

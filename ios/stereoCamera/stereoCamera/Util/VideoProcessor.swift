//
//  VideoProcessor.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/25/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation
import CoreMedia
import VideoToolbox
import AVFoundation
import Photos

class VideoProcessor
{
    private var _videoRecording = false
    var isRecording:Bool
    {
        get { return _videoRecording }
    }
    
    private var writer:AVAssetWriter?
    private var writerInput:AVAssetWriterInput?
    private var audioWriterInput:AVAssetWriterInput?
    private var videoOutputURL:URL?
    private var timestamp:Double = 0.0
    private var videoFormatDescription:CMVideoFormatDescription?
    
    func recordVideo(pixelBuffer:CVPixelBuffer, timing: CMSampleTimingInfo) {
        guard _videoRecording,
              let writer = writer
        else { return }
        
        if writer.status == .unknown {
            writer.startWriting()
            writer.startSession(atSourceTime: timing.presentationTimeStamp)
        }
        
        guard let sampleBuffer = createVideoSampleBufferWithPixelBuffer(pixelBuffer, presentationTime: timing.presentationTimeStamp)
        else {
            print("Error: Unable to create sample buffer from pixelbuffer")
            return
        }
        
        guard let writerInput = writerInput,
              writerInput.isReadyForMoreMediaData
        else { return }
        
        writerInput.append(sampleBuffer)
    }
    
    private func createVideoSampleBufferWithPixelBuffer(_ pixelBuffer: CVPixelBuffer, presentationTime: CMTime) -> CMSampleBuffer? {
        guard let videoFormatDescription = videoFormatDescription else { return nil }
        
        var sampleBuffer: CMSampleBuffer?
        var timingInfo = CMSampleTimingInfo(duration: kCMTimeInvalid, presentationTimeStamp: presentationTime, decodeTimeStamp: kCMTimeInvalid)
        
        let err = CMSampleBufferCreateForImageBuffer(kCFAllocatorDefault, pixelBuffer, true, nil, nil, videoFormatDescription, &timingInfo, &sampleBuffer)
        
        if sampleBuffer == nil {
            print("Error: Sample buffer creation failed (error code: \(err))")
        }
        
        return sampleBuffer
    }
    
    func recordAudio(sampleBuffer:CMSampleBuffer) {
        guard _videoRecording,
              let writer = audioWriterInput,
              writer.isReadyForMoreMediaData
        else { return }
        
        writer.append(sampleBuffer)
        
    }
    
    //static let ENCODED_HEIGHT:Int32 = 960
    //static let ENCODED_WIDTH:Int32 = 1920
    static let ENCODED_HEIGHT:Int32 = 2160
    static let ENCODED_WIDTH:Int32 = 4320
    
    func start(audioSettings: [String: Any]?, videoSettings: [String: Any]?, videoDescription:CMFormatDescription)
    {
        CMVideoFormatDescriptionCreate(kCFAllocatorDefault, kCMPixelFormat_32BGRA, VideoProcessor.ENCODED_WIDTH, VideoProcessor.ENCODED_HEIGHT, nil, &videoFormatDescription)
        
        guard (!_videoRecording) else { return }
        if #available(iOS 13.0, *) {
            startVideo(audioSettings: audioSettings, videoSettings: videoSettings)
        }
    }
    
    func stop()
    {
        guard _videoRecording else { return }
        _videoRecording = false
        
        //VTCompressionSessionCompleteFrames(videoSession, lastFrameTime)
        //VTCompressionSessionInvalidate(self.videoSession!)
        
        audioWriterInput?.markAsFinished()
        writerInput?.markAsFinished()
        writer?.finishWriting {
            let stat = self.writer?.status
            let err = self.writer?.error
            
            print ("video finished with \(stat) and error \(err) at \(self.writer?.outputURL)")
            
            guard err == nil else { return }
            
            if let url = self.videoOutputURL {
                Files.instance.saveVideoToPhotos(url: url) {_ in
                    print ("saved successfully")
                }
            }
            
            
            //self.videoSession = nil
            self.writer = nil
            self.writerInput = nil
            self.audioWriterInput = nil
            self._videoRecording = false
            self.videoOutputURL = nil
        }
    }
    
    @available(iOS 13.0, *)
    private func startVideo(audioSettings: [String: Any]?, videoSettings: [String: Any]?)
    {
        let fileManager = FileManager.default
        let urls = fileManager.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentDirectory: URL = urls.first else {
            fatalError("documentDir Error")
        }

        let ts = NSDate().timeIntervalSince1970
        videoOutputURL = documentDirectory.appendingPathComponent("OutputVideo\(ts).mov")
        guard let url = videoOutputURL else { return }
        if (fileManager.fileExists(atPath: url.absoluteString)) {
            do { try fileManager.removeItem(at: url) } catch { return }
        }
        
        do {
            writer = try AVAssetWriter(outputURL: videoOutputURL!, fileType: .mov)
        } catch { return }
        
        
        timestamp = Date().timeIntervalSince1970
        writerInput = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        audioWriterInput = AVAssetWriterInput(mediaType: .audio, outputSettings: audioSettings)
        audioWriterInput?.expectsMediaDataInRealTime = true
        
        writerInput!.mediaTimeScale = CMTimeScale(bitPattern: 30)
        writerInput!.expectsMediaDataInRealTime = true
        guard let writer = writer,
              writer.canAdd(writerInput!) else { return }
        writer.add(writerInput!)
        
        guard writer.canAdd(audioWriterInput!) else { return }
        writer.add(audioWriterInput!)

        _videoRecording = true
    }
}

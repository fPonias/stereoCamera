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
    var videoRecording:Bool
    {
        get { return _videoRecording }
    }
    
    private var videoSession:VTCompressionSession?
    private var writer:AVAssetWriter?
    private var writerInput:AVAssetWriterInput?
    private var audioWriterInput:AVAssetWriterInput?
    private var lastFrameTime:CMTime?
    private var videoOutputURL:URL?
    private var timestamp:Double = 0.0
    
    func addFrame(sampleBuffer:CVPixelBuffer) {
        if let session = videoSession {
            let doStart = lastFrameTime == nil ? true : false
            let interval = Double(Date().timeIntervalSince1970)
            lastFrameTime = CMTime(seconds: interval, preferredTimescale: CMTimeScale(600))
            
            if (doStart) {
                writer?.startWriting()
                writer?.startSession(atSourceTime: lastFrameTime!)
            }
            
            VTCompressionSessionEncodeFrame(session, sampleBuffer, lastFrameTime!, kCMTimeInvalid, nil, nil, nil)
        }
    }
    
    func addAudioFrame(sampleBuffer:CMSampleBuffer) {
        guard let writer = audioWriterInput,
              writer.isReadyForMoreMediaData
        else { return }
        
        writer.append(sampleBuffer)
    }
    
    private let frameEncoded:VTCompressionOutputCallback = {outputCallbackRefCon, sourceFrameRefCon, status, infoFlags, sampleBuffer in
        guard status == noErr,
              let sampleBuffer = sampleBuffer,
              let ptr = outputCallbackRefCon
        else {
            return
        }

        //debugPrint("[INFO]: outputCallback: sampleBuffer: \(sampleBuffer)")
        
        let parent = Unmanaged<VideoProcessor>.fromOpaque(ptr).takeUnretainedValue()
        
        guard let input = parent.writerInput,
              input.isReadyForMoreMediaData
        else { return }
        
        input.append(sampleBuffer)
    }
    
    func start(audioSettings: [String: NSObject]?)
    {
        guard (!videoRecording) else { return }
        if #available(iOS 13.0, *) {
            startVideo(audioSettings: audioSettings)
        }
    }
    
    func stop()
    {
        guard videoRecording,
              let videoSession = videoSession,
              let lastFrameTime = lastFrameTime
        else { return }
        
        VTCompressionSessionCompleteFrames(videoSession, lastFrameTime)
        VTCompressionSessionInvalidate(self.videoSession!)
        
        audioWriterInput?.markAsFinished()
        writerInput?.markAsFinished()
        writer?.finishWriting {
            if let url = self.videoOutputURL {
                Files.instance.saveVideoToPhotos(url: url) {_ in
                    print ("saved successfully")
                }
            }
            
            let stat = self.writer?.status
            let err = self.writer?.error
            
            print ("video finished with \(stat) and error \(err) at \(self.writer?.outputURL)")
            
            self.videoSession = nil
            self.writer = nil
            self.writerInput = nil
            self._videoRecording = false
            self.videoOutputURL = nil
        }
    }
    
    @available(iOS 13.0, *)
    private func startVideo(audioSettings: [String: NSObject]?)
    {
        let ptr = Unmanaged.passUnretained(self).toOpaque()
        
        VTCompressionSessionCreate(nil, 1920, 960, kCMVideoCodecType_H264, nil, nil, nil, self.frameEncoded, ptr, &videoSession)
        
        guard let sess = videoSession else { return }
        
        VTSessionSetProperty(sess, kVTCompressionPropertyKey_RealTime, kCFBooleanTrue)
        
        let fileManager = FileManager.default
        let urls = fileManager.urls(for: .documentDirectory, in: .userDomainMask)
        guard let documentDirectory: URL = urls.first else {
            fatalError("documentDir Error")
        }

        let ts = NSDate().timeIntervalSince1970
        videoOutputURL = documentDirectory.appendingPathComponent("OutputVideo\(ts).mp4")
        guard let url = videoOutputURL else { return }
        if (fileManager.fileExists(atPath: url.absoluteString)) {
            do { try fileManager.removeItem(at: url) } catch { return }
        }
        
        do {
            timestamp = Date().timeIntervalSince1970
            writer = try AVAssetWriter(outputURL: videoOutputURL!, fileType: .mp4)
            writerInput = try AVAssetWriterInput(mediaType: .video, outputSettings: nil, sourceFormatHint: CMFormatDescription(videoCodecType: .h264, width: 1920, height: 960))
            audioWriterInput = AVAssetWriterInput(mediaType: .audio, outputSettings: audioSettings)
            audioWriterInput?.expectsMediaDataInRealTime = true
                        
        } catch { return }
        
        writerInput!.mediaTimeScale = CMTimeScale(bitPattern: 600)
        writerInput!.expectsMediaDataInRealTime = true
        guard let writer = writer,
              writer.canAdd(writerInput!) else { return }
        writer.add(writerInput!)
        
        guard writer.canAdd(audioWriterInput!) else { return }
        writer.add(audioWriterInput!)
        

        _videoRecording = true
    }
}

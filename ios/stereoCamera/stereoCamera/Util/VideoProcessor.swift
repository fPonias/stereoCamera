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
        
        //VTCompressionSessionEncodeFrame(session, sampleBuffer, lastFrameTime!, kCMTimeInvalid, nil, nil, nil)
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
    
    /*
     formatDescription = <CMVideoFormatDescription 0x280e7b090 [0x206825860]> {
     mediaType:'vide'
     mediaSubType:'BGRA'
     mediaSpecific: {
         codecType: 'BGRA'        dimensions: 3840 x 2160
     }
     extensions: {{
     CVBytesPerRow = 15360;
     CVImageBufferColorPrimaries = "ITU_R_709_2";
     CVImageBufferTransferFunction = "ITU_R_709_2";
     CVImageBufferYCbCrMatrix = "ITU_R_709_2";
     Version = 2;
 }}
 }
     */
    
    func recordAudio(sampleBuffer:CMSampleBuffer) {
        guard _videoRecording,
              let writer = audioWriterInput,
              writer.isReadyForMoreMediaData
        else { return }
        
        //let interval = Double(Date().timeIntervalSince1970)
        //let frameTime = CMSampleBufferGetOutputPresentationTimeStamp(sampleBuffer)
        //lastFrameTime = CMTime(seconds: interval, preferredTimescale: CMTimeScale(44100))
        //CMSampleBufferSetOutputPresentationTimeStamp(sampleBuffer, lastFrameTime!)
        writer.append(sampleBuffer)
        
    }
    
    func start(audioSettings: [String: Any]?, videoSettings: [String: Any]?, videoDescription:CMFormatDescription)
    {
        CMVideoFormatDescriptionCreate(kCFAllocatorDefault, kCMPixelFormat_32BGRA, 1920, 960, nil, &videoFormatDescription)
        
        guard (!_videoRecording) else { return }
        if #available(iOS 13.0, *) {
            startVideo(audioSettings: audioSettings, videoSettings: videoSettings)
        }
    }
    
    func stop()
    {
        guard _videoRecording else { return }
        
        //VTCompressionSessionCompleteFrames(videoSession, lastFrameTime)
        //VTCompressionSessionInvalidate(self.videoSession!)
        
        audioWriterInput?.markAsFinished()
        writerInput?.markAsFinished()
        _videoRecording = false
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
        //VTCompressionSessionCreate(nil, 1920, 960, kCMVideoCodecType_HEVC, nil, nil, nil, self.frameEncoded, ptr, &videoSession)
        //guard let sess = videoSession else { return }
        //VTSessionSetProperty(sess, kVTCompressionPropertyKey_RealTime, kCFBooleanTrue)
        
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

/* recommended video settings
 ▿ 0 : 2 elements
   - key : "AVVideoWidthKey"
   - value : 1080
 ▿ 1 : 2 elements
   - key : "AVVideoHeightKey"
   - value : 1920
 ▿ 2 : 2 elements
   - key : "AVVideoCompressionPropertiesKey"
   ▿ value : 12 elements
     ▿ 0 : 2 elements
       - key : AverageBitRate
       - value : 7651584
     ▿ 1 : 2 elements
       - key : MaxKeyFrameIntervalDuration
       - value : 1
     ▿ 2 : 2 elements
       - key : Priority
       - value : 80
     ▿ 3 : 2 elements
       - key : SoftMinQuantizationParameter
       - value : 18
     ▿ 4 : 2 elements
       - key : ProfileLevel
       - value : HEVC_Main_AutoLevel
     ▿ 5 : 2 elements
       - key : AllowOpenGOP
       - value : 1
     ▿ 6 : 2 elements
       - key : AllowFrameReordering
       - value : 1
     ▿ 7 : 2 elements
       - key : MinimizeMemoryUsage
       - value : 1
     ▿ 8 : 2 elements
       - key : RelaxAverageBitRateTarget
       - value : 1
     ▿ 9 : 2 elements
       - key : MaxQuantizationParameter
       - value : 41
     ▿ 10 : 2 elements
       - key : ExpectedFrameRate
       - value : 30
     ▿ 11 : 2 elements
       - key : RealTime
       - value : 1
 ▿ 3 : 2 elements
   - key : "AVVideoCodecKey"
   - value : hvc1
 
 
 recommended audio settings
 ▿ 0 : 2 elements
   - key : "AVSampleRateKey"
   - value : 44100
 ▿ 1 : 2 elements
   - key : "AVEncoderBitRatePerChannelKey"
   - value : 96000
 ▿ 2 : 2 elements
   - key : "AVFormatIDKey"
   - value : 1633772320
 ▿ 3 : 2 elements
   - key : "AVEncoderBitRateStrategyKey"
   - value : AVAudioBitRateStrategy_Variable
 ▿ 4 : 2 elements
   - key : "AVNumberOfChannelsKey"
   - value : 1
 ▿ 5 : 2 elements
   - key : "AVEncoderQualityForVBRKey"
   - value : 91
 */

import UIKit
import Photos

class GalleryPlaybackCtrl: UIViewController, UIDocumentInteractionControllerDelegate
{
    @IBOutlet weak var image: UIImageView!
    @IBOutlet weak var playBtn: UIButton!
    
    var files = [PHAsset]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        files = Files.getGalleryFiles()
        index = _startIndex
        image.image = Files.assetToImage(files[_startIndex])
        
        let leftSwipeRec = UISwipeGestureRecognizer(target: self, action: #selector(GalleryPlaybackCtrl.leftSwipe))
        leftSwipeRec.direction = .left
        leftSwipeRec.numberOfTouchesRequired = 1
        image.addGestureRecognizer(leftSwipeRec)
        
        let rightSwipeRec = UISwipeGestureRecognizer(target: self, action: #selector(GalleryPlaybackCtrl.rightSwipe))
        rightSwipeRec.direction = .right
        rightSwipeRec.numberOfTouchesRequired = 1
        image.addGestureRecognizer(rightSwipeRec)
    }
    
    private var _startIndex:Int = 0
    var startIndex:Int
    {
        get { return _startIndex}
        set { _startIndex = newValue }
    }
    
    private var commandSenderCondition = NSCondition()
    private var isPlaying:Bool = false
    static let DELAY = 2.5
    private var index = 0;
    
    @IBAction func playClicked(_ sender: Any)
    {
        isPlaying = !isPlaying
        
        if (isPlaying)
        {
            playBtn.setImage(UIImage(named: "pause"), for: .normal)
            DispatchQueue.global(qos: .userInitiated).async {
            [unowned self] in
                self.playLoop()
            }
        }
        else
        {
            cancelPlay()
        }
    }
    
    func cancelPlay()
    {
        playBtn.setImage(UIImage(named: "play_arrow"), for: .normal)
        commandSenderCondition.lock()
            if (isPlaying)
            {
                isPlaying = false
                commandSenderCondition.signal()
            }
        commandSenderCondition.unlock()
    }
    
    private func playLoop()
    {
        if (index == files.count - 1)
        {
            DispatchQueue.main.async
            {
                self.setIndex(0)
            }
        }
        
        while (isPlaying)
        {
            var doReturn = false
            
            commandSenderCondition.lock()
                commandSenderCondition.wait(until: Date(timeIntervalSinceNow: TimeInterval(GalleryPlaybackCtrl.DELAY)))
            
                if (!isPlaying)
                    { doReturn = true }
            commandSenderCondition.unlock()
            
            if (doReturn)
                { return; }
            
            DispatchQueue.main.async {
            [unowned self ] in
                self.setIndex(self.index + 1)
            }
        }
    }
    
    private func setIndex(_ next:Int)
    {
        let max = files.count
        
        if (next == max)
        {
            cancelPlay()
            return
        }
        
        index = next;
        
        let anim = UIViewPropertyAnimator(duration: TimeInterval(0.15), curve: UIViewAnimationCurve.linear, animations: {() -> Void in
            self.image.alpha = 0.0
        })
        
        anim.addCompletion({ posititon in
            self.setIndex2(next)
        })
        
        anim.startAnimation()
    }
    
    private func setIndex2(_ next: Int)
    {
        let anim = UIViewPropertyAnimator(duration: TimeInterval(0.15), curve: UIViewAnimationCurve.linear, animations: {() -> Void in
            self.image.alpha = 1.0
        })
        
        self.image.image = Files.assetToImage(self.files[next])
        anim.startAnimation()
    }
    
    @objc func leftSwipe()
    {
        if (index < files.count - 2)
            { setIndex(index + 1 ) }
    }
    
    @objc func rightSwipe()
    {
        if (index > 0)
            { setIndex(index - 1) }
    }
    
    @IBAction func exportAction(_ sender: Any)
    {
        cancelPlay()
        
        guard (image.image != nil) else
        {
            print("invalid image selected")
            return
        }
        
        let imgToShare = [ ImageProvider(parent: self, placeholderItem: image.image!) ]
        let shareCtrl = UIActivityViewController(activityItems: imgToShare, applicationActivities: nil)
        present(shareCtrl, animated: true, completion: nil)
    }
    
    @IBAction func trashAction(_ sender: Any)
    {
        cancelPlay()
        
        let file = files[index]
        let result = Files.deleteAssets([file])
        
        if (!result)
        {
            print("failed to delete image")
            return
        }
        
        if (index == files.count - 1)
            { index -= 1 }

        files = Files.getGalleryFiles()
        setIndex(index)
    }
    
    class ImageProvider : UIActivityItemProvider
    {
        var parent:GalleryPlaybackCtrl
        var target:UIImage
    
        init(parent:GalleryPlaybackCtrl, placeholderItem:UIImage)
        {
            self.parent = parent
            self.target = placeholderItem
            super.init(placeholderItem: placeholderItem)
        }
    
        override var item: Any
        {
            get
            {
                print ("for activity type " + activityType.debugDescription)
                
                let title = activityType?.rawValue
                if (title == "com.burbn.instagram.shareextension")
                {
                    return getInstagramImage()
                }
                else
                {
                    return target
                }
            }
        }
                
        func getInstagramImage() -> UIImage
        {
            print ("converting export image")
            //convert the preview image to a mutable image
            let ciImag = CIImage(image: target)
            guard ciImag != nil else { return target }
            let ciImage = ciImag!
            
            let context = CIContext(options: nil)
            let cgImag = context.createCGImage(ciImage, from: ciImage.extent)
            guard cgImag != nil else { return target }
            let cgImage = cgImag!

            //setup the drawing context and match the source image encoding parameters
            let outDims = CGSize(width: 1080, height: 1350) //instagram max portrait dimensions
            let outContex = CGContext.init(data: nil, width: Int(outDims.width), height: Int(outDims.height), bitsPerComponent: cgImage.bitsPerComponent, bytesPerRow: cgImage.bitsPerPixel * Int(outDims.width), space: cgImage.colorSpace!, bitmapInfo: cgImage.bitmapInfo.rawValue)
            
            guard outContex != nil else { return target }
            let outContext = outContex!
            
            //draw a white background
            let outRect = CGRect(x: 0, y: 0, width: outDims.width, height: outDims.height)
            outContext.setFillColor(UIColor.white.cgColor)
            outContext.fill(outRect)
            
            //draw the source image to scale and rotate to Instagram image sizing parameters
            //guess n' check matrix rotation and translation
            let scaledHeight = outDims.height / CGFloat(cgImage.width) * CGFloat(cgImage.height)
            outContext.rotate(by: -CGFloat.pi / 2.0)
            outContext.translateBy(x: -outDims.height, y: (outDims.width - scaledHeight) * 0.5)
            let imgRect = CGRect(x: 0, y: 0, width: outDims.height, height: scaledHeight)
            outContext.draw(cgImage, in: imgRect)
            
            let newImg = outContext.makeImage()
            let ret = UIImage(cgImage: newImg!)

            print ("converted!")
            return ret
        }
    }
}

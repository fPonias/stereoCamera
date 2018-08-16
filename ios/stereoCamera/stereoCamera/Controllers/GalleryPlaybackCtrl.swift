import UIKit
import Photos

class GalleryPlaybackCtrl: UIViewController, UIDocumentInteractionControllerDelegate, UIScrollViewDelegate, UIGestureRecognizerDelegate
{
    @IBOutlet weak var scroller: UIScrollView!
    @IBOutlet weak var toolbar: UIToolbar!
    @IBOutlet weak var playBtn: UIButton!
    @IBOutlet weak var toolbarHeightConstraint: NSLayoutConstraint!
    @IBOutlet weak var toolbarTitle: UIBarButtonItem!
    
    var files = [PHAsset]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        files = Files.getGalleryFiles()
        index = _startIndex
        
        toolbarHeight = toolbarHeightConstraint.constant
        
        
        
        doInit()
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
    private var index = 0
    private var toolbarHeight:CGFloat = 44
    
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
            
            toolbarHeightConstraint.constant = 0
            UIView.animate(withDuration: 0.25){
            [unowned self] in
                self.toolbar.layoutIfNeeded()
            }
        }
        else
        {
            cancelPlay()
            
            toolbarHeightConstraint.constant = toolbarHeight
            UIView.animate(withDuration: 0.25){
            [unowned self] in
                self.toolbar.layoutIfNeeded()
            }
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
                //self.setIndex(0)
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
        gotoPage(page: next, animated: true)
    }
    
    @IBAction func exportAction(_ sender: Any)
    {
        cancelPlay()
        
        let img = Files.assetToImage(files[index])
        let imgToShare = [ ImageProvider(parent: self, placeholderItem: img) ]
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
        setImage(page: index, image: slides.slides[2])
        setImage(page: index + 1, image: slides.slides[3])
        setImage(page: index + 2, image: slides.slides[4])
        
        let w = view.frame.width
        scroller.contentSize.width = w * CGFloat(files.count)
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
    
    struct ImageView
    {
        var page:UIView? = nil
        var imgView:UIImageView? = nil
        var widthConst:NSLayoutConstraint? = nil
        var heightConst:NSLayoutConstraint? = nil
        var topConst:NSLayoutConstraint? = nil
        var leadConst:NSLayoutConstraint? = nil
    }
    
    struct Slides
    {
        var slides = [ImageView]()
        var leftConstraints = [NSLayoutConstraint?]()
        var centerIndex = 0
    }
    
    var leftPage:UIView? = nil
    var slides = Slides()
    var tapRecognizer:UITapGestureRecognizer? = nil
    
    func doInit()
    {
        let w = view.frame.width
        var h = view.frame.height - (navigationController?.navigationBar.frame.size.height)!
        let sz = files.count
        
        scroller.contentSize = CGSize(width: w * CGFloat(sz), height: h)
        scroller.delegate = self
        scroller.contentInsetAdjustmentBehavior = .never
        
        tapRecognizer = UITapGestureRecognizer(target: self, action: #selector(GalleryPlaybackCtrl.onTap))
        tapRecognizer!.cancelsTouchesInView = false
        scroller.addGestureRecognizer(tapRecognizer!)
        tapRecognizer?.delegate = self
        
        for _ in 1 ... 5
        {
            slides.slides.append(createPage())
            slides.leftConstraints.append(nil)
        }
        
        setImage(page: 0, image: slides.slides[2])
        setImage(page: 1, image: slides.slides[3])
        setImage(page: 2, image: slides.slides[4])
        
        
        gotoPage(page: index, animated: false)
    }
    
    @objc func onTap()
    {
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool
    {
        let r = playBtn.frame
        let t = touch.location(in: view)
        if (r.contains(t))
        {
            playClicked(self)
        }
        
        return false
    }

    // MARK: - Page Loading

    fileprivate func createPage() -> ImageView
    {
        let w = view.frame.width
        var h = view.frame.height - (navigationController?.navigationBar.frame.size.height)!
        
        let newImageView = UIImageView(image: nil)
        newImageView.contentMode = .scaleAspectFit
        
        let frame = CGRect(x: 0, y: 0, width: w, height: h)
        let canvasView = UIView(frame: frame)
        scroller.addSubview(canvasView)
        
        newImageView.translatesAutoresizingMaskIntoConstraints = false
        canvasView.addSubview(newImageView)
        
        
        let constraints = [
            (newImageView.widthAnchor.constraint(equalToConstant: 100)),
            (newImageView.heightAnchor.constraint(equalToConstant: 100)),
            (newImageView.topAnchor.constraint(equalTo: scroller.topAnchor, constant: 0)),
            (newImageView.leadingAnchor.constraint(equalTo: scroller.leadingAnchor, constant: 0))
        ]
        NSLayoutConstraint.activate(constraints)
        
        return ImageView(page: canvasView, imgView: newImageView, widthConst: constraints[0], heightConst: constraints[1], topConst: constraints[2], leadConst: constraints[3])
    }
    
    func setImage(page:Int, image img:ImageView)
    {
        if (page < 0 || page > files.count - 1)
        {
            img.imgView!.image = nil
            return
        }
        
        let image = Files.assetToImage(files[page])
        img.imgView!.image = image
        let isz = image.size
        let idim = CGFloat.maximum(isz.width, isz.height)
        
        let top = (navigationController?.navigationBar.frame.size.height)!
        let w = view.frame.width
        var h = view.frame.height
        let fdim = CGFloat.minimum(w, h)
        
        img.widthConst?.constant = fdim
        img.heightConst?.constant = fdim

        if (isz.width > isz.height)
        {
            let margin = (h - fdim) * 0.5 - top
            img.topConst?.constant = margin
            
            let offset = w * CGFloat(page)
            img.leadConst?.constant = offset
        }
        else
        {
            img.topConst?.constant = 0
            
            let margin = (w - fdim) * 0.5
            let offset = w * CGFloat(page)
            img.leadConst?.constant = margin + offset
        }
        
        img.imgView!.setNeedsLayout()
    }

    fileprivate func loadCurrentPages()
    {
        if (index == slides.centerIndex)
            { return }
    
        if (index > slides.centerIndex)
        {
            let diff = index - slides.centerIndex
            for _ in 1 ... diff
            {
                shiftRight()
            }
        }
        else if (index < slides.centerIndex)
        {
            let diff = slides.centerIndex - index
            for _ in 1 ... diff
            {
                shiftLeft()
            }
        }
    }
    
    private func shiftRight()
    {
        let img = slides.slides.remove(at: 0)
        slides.centerIndex += 1
        setImage(page: slides.centerIndex + 2, image: img)
        slides.slides.append(img)
    }
    
    private func shiftLeft()
    {
        let img = slides.slides.remove(at: 4)
        slides.centerIndex -= 1
        setImage(page: slides.centerIndex - 2, image: img)
        slides.slides.insert(img, at: 0)
    }

    fileprivate func gotoPage(page: Int, animated: Bool)
    {
        let w = view.frame.width
        index = page
        navigationItem.title = "Image " + String(index + 1) + "/" + String(files.count)
        
        loadCurrentPages()
        
        var bounds = scroller.bounds
        bounds.origin.x = w * CGFloat(page)
        bounds.origin.y = 0
        scroller.scrollRectToVisible(bounds, animated: animated)
    }
    
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView)
    {
        let offset = scroller.contentOffset
        let pageRaw = offset.x / view.frame.width
        index = Int(pageRaw.rounded())
        navigationItem.title = "Image " + String(index + 1) + "/" + String(files.count)
        loadCurrentPages()
    }
    
    func scrollViewDidScroll(_ scrollView: UIScrollView)
    {
        scroller.contentOffset.y = 0
    }
}

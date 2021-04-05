import UIKit
import Photos

class GalleryPlaybackCtrl: UIViewController, UIDocumentInteractionControllerDelegate, UIScrollViewDelegate, UIGestureRecognizerDelegate
{
    @IBOutlet weak var scroller: UIScrollView!
    @IBOutlet weak var toolbar: UIToolbar!
    @IBOutlet weak var playBtn: UIButton!
    @IBOutlet weak var toolbarHeightConstraint: NSLayoutConstraint!
    
    var files = [PHAsset]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        files = Files.instance.getSortedGalleryFiles()
        index = _startIndex
        
        toolbarHeight = toolbarHeightConstraint.constant
    }
    
    override func viewDidLayoutSubviews() {
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
                self.resize()
            }
        }
        else
        {
            cancelPlay()
            
            toolbarHeightConstraint.constant = toolbarHeight
            UIView.animate(withDuration: 0.25){
            [unowned self] in
                self.resize()
            }
        }
    }
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator)
    {
        coordinator.animateAlongsideTransition(in: self.view, animation: nil, completion: {
        [unowned self] (context) in
            self.resize()
            self.gotoPage(page: self.index, animated: false)
        })
    }
    
    func resize()
    {
        toolbar.layoutIfNeeded()
        
        for i in 0 ... 4
        {
            let idx = index + (i - 2)
            setImage(page: idx, image: slides.slides[i])
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
    
    private let SELECTION_COPY = "Copy"
    private let SELECTION_ROTATE_INSTA = "Rotate for Instagram"
    private let SELECTION_SQUARE = "Square aspect"
    private let SELECTION_SQUARE_ROTATED = "Square aspect - rotated"
    
    @IBAction func exportAction(_ sender: Any)
    {
        cancelPlay()
        
        let popup = ExportSelectCtrl.initFromStoryboard()
        
        popup.header = "Export Actions"

        var selections = [ExportSelectCtrl.Selection]()
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_COPY))
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_ROTATE_INSTA))
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_SQUARE))
        selections.append(ExportSelectCtrl.Selection(text: SELECTION_SQUARE_ROTATED))
        popup.setSelections(selections)
        
        popup.addAction(action: ExportSelectCtrl.Action(text: "Cancel", onClick: {
        [unowned self] (ctrl, action) in
            self.dismiss(animated: true, completion: nil)
        }))
        
        popup.addAction(action: ExportSelectCtrl.Action(text: "Export", onClick: {
        [unowned self] (ctrl, action) in
            
            var type = ImageProvider.ExportType.COPY
            let selection = popup.currentSelection
            
            if (selection != nil)
            {
                if (selection!.text == self.SELECTION_ROTATE_INSTA)
                    { type = ImageProvider.ExportType.ROTATE_TO_PORTRAIT }
                else if (selection!.text == self.SELECTION_SQUARE_ROTATED)
                    { type = ImageProvider.ExportType.ROTATE_TO_SQUARE }
                else if (selection!.text == self.SELECTION_SQUARE)
                    { type = ImageProvider.ExportType.SQUARE }
                else
                    { type = ImageProvider.ExportType.COPY }
            }
            
            self.dismiss(animated: true, completion: nil)
            self.exportClicked2(type)
        }))
        
        present(popup, animated: true, completion: nil)
    }

    func exportClicked2(_ type:ImageProvider.ExportType)
    {
        var toShare = [ImageProvider]()
        let file = files[index]
        let img = Files.instance.assetToImage(file)
        toShare.append( ImageProvider(placeholderItem: img, type: type ) )
        
        let shareCtrl = UIActivityViewController(activityItems: toShare, applicationActivities: nil)
        present(shareCtrl, animated: true, completion: nil)
    }
    
    @IBAction func trashAction(_ sender: Any)
    {
        cancelPlay()
        
        let file = files[index]
        let result = Files.instance.deleteAssets([file])
        
        if (!result)
        {
            print("failed to delete image")
            return
        }
        
        if (index == files.count - 1)
            { index -= 1 }

        files = Files.instance.getGalleryFiles()
        setImage(page: index, image: slides.slides[2])
        setImage(page: index + 1, image: slides.slides[3])
        setImage(page: index + 2, image: slides.slides[4])
        
        let w = view.frame.width
        scroller.contentSize.width = w * CGFloat(files.count)
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
        let w = scroller.frame.width
        var h = scroller.frame.height
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
        let w = scroller.frame.width
        let h = scroller.frame.height
        
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
        
        let image = Files.instance.assetToImage(files[page])
        img.imgView!.image = image
        let w = scroller.frame.width
        let h = scroller.frame.height

        img.widthConst?.constant = w
        img.heightConst?.constant = h
        img.topConst?.constant = 0
        
        let offset = w * CGFloat(page)
        img.leadConst?.constant = offset
        
        img.imgView!.setNeedsLayout()
    }

    fileprivate func loadCurrentPages()
    {
        if (index == slides.centerIndex)
            { return }
        
        var bigdiff = index - slides.centerIndex
        bigdiff = (bigdiff > 0) ? bigdiff : -bigdiff
        
        if (bigdiff < 3)
        {
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
        else
        {
            slides.centerIndex = index
            for i in 0 ... 4
            {
                let idx = index + ( i - 2 )
                setImage(page: idx, image: slides.slides[i])
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
        var bounds = scroller.bounds
        let w = bounds.width
        index = page
        navigationItem.title = "Image " + String(index + 1) + "/" + String(files.count)
        
        loadCurrentPages()
        
        bounds.origin.x = w * CGFloat(page)
        bounds.origin.y = 0
        scroller.scrollRectToVisible(bounds, animated: animated)
    }
    
    func scrollViewDidEndDecelerating(_ scrollView: UIScrollView)
    {
        cancelPlay()
    
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

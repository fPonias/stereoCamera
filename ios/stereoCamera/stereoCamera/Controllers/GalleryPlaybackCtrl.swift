import UIKit
import Photos

class GalleryPlaybackCtrl: UIViewController, UIDocumentInteractionControllerDelegate, UIScrollViewDelegate, UIGestureRecognizerDelegate
{
    
    @IBOutlet weak var scroller: UIView!
    @IBOutlet weak var scrollerContent: UIView!
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
    
    private var _startIndex:Int = 0
    var startIndex:Int
    {
        get { return _startIndex}
        set { _startIndex = newValue }
    }
    
    private var commandSenderCondition = NSCondition()
    private var isPlaying:Bool = false
    static let DELAY = 2.5
    private var _index:Int = -1
    private var index:Int {
        get { return _index }
        set {
            print("setting index to \(newValue)")
            _index = newValue
            loadCurrentPages(index: newValue)
        }
    }
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
        })
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        doInit()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(false, animated: animated)
        
        scroller.clipsToBounds = true
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        
        scroller.clipsToBounds = false
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.setNavigationBarHidden(true, animated: animated)
        
        for key in renderedSlides.keys {
            renderedSlides[key]?.deselected()
        }
        
        scroller.clipsToBounds = true
    }
    
    func resize()
    {
        toolbar.layoutIfNeeded()
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
                self.setIndex(_index + 1)
            }
        }
    }
    
    private var panStarted = Date()
    private var panStartX:CGFloat = 0.0
    private var panEndVelocity:Double = 0.0
    private var panAnimator:UIViewPropertyAnimator? = nil
    
    @IBAction func onPan(_ sender: Any) {
        guard let recog = sender as? UIPanGestureRecognizer else { return }
        
        if (recog.state == .began) {
            panStarted = Date()
            panStartX = scrollerContent.frame.origin.x
            
            if (panAnimator != nil) {
                panAnimator?.stopAnimation(true)
                panAnimator = nil
                
                loadCurrentPages()
            }
        }
        else if (recog.state == .ended) {
            let elapsed = Date().timeIntervalSince(panStarted)
            let velPnt = recog.translation(in: scroller)
            let velocity = Double(velPnt.x) / elapsed
            panEndVelocity = velocity
            
            print("velocity \(velocity)")
            
            let delta = (panEndVelocity > 0) ? -1 : 1
            slideBy(start: panStartX, delta: delta)
        } else {
            let distPnt = recog.translation(in: scroller)
            
            //print("dist \(distPnt.x)")
            
            let rect = scrollerContent.frame
            scrollerContent.frame = CGRect(x: panStartX + distPnt.x, y: 0, width: rect.width, height: rect.height)
        }
    }
    
    @IBAction func onLeftSwipe(_ sender: Any) {
        index -= 1
    }
    
    @IBAction func onRightSwipe(_ sender: Any) {
        index += 1
    }
    
    private func setIndex(_ next:Int)
    {
        let max = files.count
        
        if (next == max)
        {
            cancelPlay()
            return
        }
        
        slideBy(start: scrollerContent.frame.origin.x, delta: next - _index)
    }
    
    private func slideBy(start:CGFloat, delta:Int)
    {
        let x = scrollerContent.frame.origin.x
        let w = scroller.frame.width
        
        var nextX = start + CGFloat(delta) * -w
        let nextIdx = Int(-nextX / w) + _index
        
        let duration:CGFloat
        if (nextIdx < 0 || nextIdx >= files.count) {
            nextX = 0
            let diff = nextX - x
            duration = 0.5 / 1200.0 * diff
        } else {
            let diff = nextX - x
            duration = 0.4 / 1200.0 * diff
        }
        
        panAnimator = UIViewPropertyAnimator(duration: Double(abs(duration)), curve: .easeOut) { [weak self] in
            guard let self = self else { return }
            self.scrollerContent.frame.origin.x = nextX
        }
        panAnimator?.addCompletion { [weak self] (position) in
            self?.loadCurrentPages()
        }
        panAnimator?.startAnimation()
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
        Files.instance.assetToImage(file, completed: {[weak self] img in
            guard let self = self,
                let image = img
            else { return }
            
            toShare.append( ImageProvider(placeholderItem: image, type: type ) )
            
            let shareCtrl = UIActivityViewController(activityItems: toShare, applicationActivities: nil)
            self.present(shareCtrl, animated: true, completion: nil)
        })
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
    }
    
    var imageCache:[ImageView] = Array()
    var movieCache:[VideoView] = Array()
    var toggleCache:[ToggleView] = Array()
    
    var renderedSlides = [Int:PreviewView]()
    let span = 3
    
    func getPage(page:Int) -> PreviewView?
    {
        guard (page >= 0 && page <= files.count - 1) else { return nil }
        
        let w = scroller.frame.width
        let h = scroller.frame.height
        let left = getLeft()
        let offset = CGFloat(page - left) * w
        let frame = CGRect(x: offset, y: 0, width: w, height: h)
        
        let rendered = renderedSlides[page]
        if (rendered != nil) {
            rendered?.frame = frame
            scrollerContent.setNeedsLayout()
            
            return rendered
        }
        
        let thisFile = files[page]
        let ret:PreviewView
        
        if (thisFile.mediaType == .video) {
            if (movieCache.isEmpty) {
                ret = VideoView()
            } else {
                ret = movieCache.removeLast()
            }
        } else if (thisFile.mediaType == .image && thisFile.playbackStyle == .imageAnimated) {
            if (toggleCache.isEmpty) {
                ret = ToggleView()
            } else {
                ret = toggleCache.removeLast()
            }
        } else {
            if (imageCache.isEmpty) {
                ret = ImageView()
            } else {
                ret = imageCache.removeLast()
            }
        }
        
        ret.frame = frame
        ret.setImage(thisFile: thisFile)
        scrollerContent.addSubview(ret)
        scrollerContent.setNeedsLayout()
        
        return ret
    }
    
    class PreviewView : UIView
    {
        var page:Int = -1
        var thisFile:PHAsset? = nil
        
        override init(frame:CGRect) {
            super.init(frame: frame)
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        func setImage(thisFile:PHAsset) {
            self.thisFile = thisFile
        }
        
        var isSelected = false
        
        func selected() { isSelected = true }
        func deselected() { isSelected = false }
    }
    
    class ImageView : PreviewView
    {
        var imageView:UIImageView
        var image:UIImage? = nil
        
        var widthConstraint:NSLayoutConstraint? = nil
        var heightConstraint:NSLayoutConstraint? = nil
        
        override init(frame:CGRect) {
            imageView = UIImageView(frame:frame)
            
            super.init(frame:frame)
            addSubview(imageView)
        }
        
        required init?(coder: NSCoder) {
            imageView = UIImageView(frame:CGRect())
            super.init(coder: coder)
        }
        
        override func setImage(thisFile:PHAsset) {
            Files.instance.assetToImage(thisFile, completed: { [weak self] image in
                guard let self = self,
                      let image = image
                else { return }
                
                self.imageView.contentMode = .center
                self.imageView.contentMode = .scaleAspectFit
                
                self.image = image
                self.imageView.image = image
                
                self.setNeedsLayout()
            })
        }
        
        override var frame: CGRect {
            get { return super.frame }
            set {
                super.frame = newValue
                
                let imgFrame = CGRect(x: 0, y: 0, width: newValue.width, height: newValue.height)
                self.imageView.frame = imgFrame
            }
        }
    }
    
    class VideoView : PreviewView
    {
        var player = AVPlayer()
        var playerLayer:AVPlayerLayer? = nil
        
        override init(frame:CGRect) {
            super.init(frame:frame)
        }
        
        required init?(coder: NSCoder) {
            super.init(coder: coder)
        }
        
        
        override var frame: CGRect {
            get { return super.frame }
            set {
                super.frame = newValue
                
                let imgFrame = CGRect(x: 0, y: 0, width: newValue.width, height: newValue.height)
                bounds = imgFrame
                setupLayer()
            }
        }
        
        override func setImage(thisFile:PHAsset)
        {
            Files.instance.assetToUrl(thisFile, completed: { [weak self] (videoURL) in
                guard let url = videoURL,
                      let self = self
                else { return }
                
                self.player = AVPlayer(url: url)
                self.setupLayer()
                
                if (self.isSelected) {
                    self.selected()
                }
            })
        }
        
        private func setupLayer() {
            if playerLayer != nil {
                playerLayer?.removeFromSuperlayer()
            }
            
            playerLayer = AVPlayerLayer.init(player: player)
            guard let pllayer = playerLayer else { return }
            
            pllayer.frame = bounds
            pllayer.videoGravity = .resizeAspect
            pllayer.needsDisplayOnBoundsChange = true
            
            layer.addSublayer(pllayer)
            layer.needsDisplayOnBoundsChange = true
        }
        
        override func selected() {
            super.selected()
            player.play()
        }
        
        override func deselected() {
            super.deselected()
            player.pause()
        }
    }
    
    class ToggleView : ImageView
    {
        override func setImage(thisFile:PHAsset) {
            Files.instance.assetToData(thisFile, completed: { [weak self] data in
                guard let self = self,
                      let data = data
                else { return }
                
                self.imageView.contentMode = .center
                self.imageView.contentMode = .scaleAspectFit
                
                guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return }
                
                var images = [UIImage]()
                let imageCount = CGImageSourceGetCount(source)
                for i in 0 ..< imageCount {
                    if let image = CGImageSourceCreateImageAtIndex(source, i, nil) {
                        images.append(UIImage(cgImage: image))
                    }
                }
                
                self.imageView.animationImages = images
                self.imageView.animationDuration = 0.4
                self.imageView.animationRepeatCount = 0
                
                if (self.isSelected) {
                    self.selected()
                }
                
                self.setNeedsLayout()
            })
        }
        
        override func selected() {
            super.selected()
            imageView.startAnimating()
        }
        
        override func deselected() {
            super.deselected()
            imageView.stopAnimating()
        }
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        doInit()
    }
    
    func doInit()
    {
        let w = scroller.frame.width
        let h = scroller.frame.height
        guard (scrollerContent.frame.height != h) else { return }
        
        scrollerContent.frame.size.width = w * CGFloat(span)
        scrollerContent.frame.size.height = h
        view.isUserInteractionEnabled = true
        loadCurrentPages(force: true)
    }
    
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        true
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
    fileprivate func getLeft() -> Int {
        return getLeft(index: _index)
    }
    
    fileprivate func getLeft(index idx:Int) -> Int {
        let center = (span - 1) / 2
        let lft = idx - center
        let left = max(0, lft)
        
        return left
    }
    
    fileprivate func loadCurrentPages(index: Int) {
        _index = index
        loadCurrentPages2()
    }
    
    fileprivate func loadCurrentPages(force: Bool = false)
    {
        let changed = resetIndex()
        guard force || changed else { return }
        
        loadCurrentPages2()
    }
    
    fileprivate func loadCurrentPages2()
    {
        let left = getLeft()
        let right = min(left + span - 1, files.count - 1)
        
        for page in left ... right {
            let slide = getPage(page: page)
            renderedSlides[page] = slide
            
            if (page == _index) {
                slide?.selected()
            } else {
                slide?.deselected()
            }
        }
        
        cleanUpPages()
        
        let w = scroller.frame.width
        let offset = -1.0 * CGFloat(_index - left) * w
        scrollerContent.frame.origin.x = offset
        
        scrollerContent.setNeedsLayout()
    }
    
    
    
    fileprivate func resetIndex() -> Bool {
        let width = scroller.frame.width
        let actualOffset = Int(-round(scrollerContent.frame.origin.x / width))
        let left = getLeft()
        let nextIndex = left + actualOffset
        
        let ret = (_index != nextIndex)
        
        _index = nextIndex
        
        return ret
    }
    
    fileprivate func cleanUpPages()
    {
        let left = getLeft()
        let right = left + span - 1
        for page in renderedSlides.keys
        {
            if (page >= left && page <= right) {
                continue
            }
            
            print("recycling page \(page)")
            if let slide = renderedSlides[page] {
                slide.deselected()
                slide.removeFromSuperview()
                
                if let videoSlide = slide as? VideoView {
                    movieCache.append(videoSlide)
                } else if let photoSlide = slide as? ImageView {
                    imageCache.append(photoSlide)
                } else if let toggleSlide = slide as? ToggleView {
                    toggleCache.append(toggleSlide)
                }
                
                renderedSlides[page] = nil
            }
        }
    }
}

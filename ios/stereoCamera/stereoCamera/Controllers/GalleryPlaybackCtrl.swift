import UIKit
import Photos

class GalleryPlaybackCtrl: UIViewController
{
    @IBOutlet weak var image: UIImageView!
    @IBOutlet weak var playBtn: UIButton!
    
    var files = [PHAsset]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        files = Files.getGalleryFiles()
        image.image = Files.assetToImage(files[0])
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        guard (sender is IndexPath) else { return }
        let idx:IndexPath = sender as! IndexPath
        
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
            DispatchQueue.global(qos: .userInitiated).async
            {
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
            
            DispatchQueue.main.async
            {
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
}

import UIKit
import Photos

class GalleryGridCtrl: UIViewController, UICollectionViewDelegate, UICollectionViewDataSource
{
    static func initFromStoryboard() -> GalleryGridCtrl
    {
        let sb = UIStoryboard.init(name: "Main", bundle: nil)
        let ret = sb.instantiateViewController(withIdentifier: "GalleryGrid")
    
        return ret as! GalleryGridCtrl
    }
    
    @IBOutlet weak var exportBtn: UIBarButtonItem!
    @IBOutlet weak var trashBtn: UIBarButtonItem!
    
    @IBOutlet weak var bottomToolbar: UIToolbar!
    @IBOutlet weak var collectionView: UICollectionView!
    var files = [PHAsset]()
    
    var selectBtn:UIBarButtonItem? = nil
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        collectionView.dataSource = self
        collectionView.delegate = self
        
        navigationItem.title = "Gallery"
        selectBtn = UIBarButtonItem(title: "Select", style: .plain, target: self, action: #selector(GalleryGridCtrl.selectClicked))
        navigationItem.rightBarButtonItem = selectBtn!
        
        trashBtn.target = self
        trashBtn.action = #selector(GalleryGridCtrl.trashClicked)
        
        exportBtn.target = self
        exportBtn.action = #selector(GalleryGridCtrl.exportClicked)
    }
    
    override func viewWillAppear(_ animated: Bool)
    {
        files = Files.getGalleryFiles()
        
        DispatchQueue.main.async {
        [unowned self] in
            self.collectionView.reloadData()
        }
    }
    
    @objc func trashClicked()
    {
        if (selectedCells.count == 0)
            { return }
        
        var indices = [PHAsset]()
        for cell in selectedCells
        {
            indices.append(files[cell])
        }
        
        let result = Files.deleteAssets(indices)
        
        if (result == true)
        {
            selectClicked()
            files = Files.getGalleryFiles()
            
            DispatchQueue.main.async {
            [unowned self] in
                self.collectionView.reloadData()
            }
        }
    }
    
    @objc func exportClicked()
    {
    
    }
    
    private var isSelecting = false
    private var selectedCells = Set<Int>()
    
    @objc func selectClicked()
    {
        if (isSelecting)
        {
            isSelecting = false
            selectBtn?.title = "Select"
            navigationItem.title = "Gallery"
            
            for idx in selectedCells
            {
                let indexPath = IndexPath(item: idx, section: 0)
                let cell = collectionView.cellForItem(at: indexPath)
                
                if (cell != nil)
                {
                    let widget = cell as! GalleryGridWidget
                    widget.isHighlighted = false
                }
            }
            
            selectedCells.removeAll()
            
            bottomToolbar.isHidden = true
        }
        else
        {
            isSelecting = true
            selectBtn?.title = "Cancel"
            bottomToolbar.isHidden = false
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int
    {
        return files.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell
    {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "galleryGridWidget", for: indexPath)
        guard (cell is GalleryGridWidget) else { return cell }
        
        let cell2 = cell as! GalleryGridWidget
        
        
        let file:PHAsset = files[indexPath.item]
        let image:UIImage = Files.assetToImage(file, asThumbnail: true)
        cell2.displayContent(image: image)
        let selected = selectedCells.contains(indexPath.item)
        cell2.isHighlighted = selected

        return cell2
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath)
    {
        if (!isSelecting)
        {
            performSegue(withIdentifier: "GalleryToPlayback", sender: indexPath.item)
            return
        }
        
        
        let cell = collectionView.cellForItem(at: indexPath)
        let widget = cell as! GalleryGridWidget
        let selected = (selectedCells.contains(indexPath.item)) ? false : true
        
        widget.isHighlighted = selected
        
        if (selected)
            { selectedCells.insert(indexPath.item) }
        else
            { selectedCells.remove(indexPath.item) }
        
        navigationItem.title = String(selectedCells.count) + " Selected"
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?)
    {
        if (segue.identifier == "GalleryToPlayback")
        {
            let ctrl = segue.destination as! GalleryPlaybackCtrl
            ctrl.startIndex = sender as! Int
        }
    }
}

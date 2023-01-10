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
    var headers = [String]()
    var files = [[PHAsset]]()
    
    var selectBtn:UIBarButtonItem? = nil
    var filterBtn:UIBarButtonItem? = nil
    var sortBtn:UIBarButtonItem? = nil
    var cancelBtn:UIBarButtonItem? = nil
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.allowsMultipleSelection = true
        
        navigationItem.title = "Gallery"
        
        selectBtn = UIBarButtonItem(title: "Select", style: .plain, target: self, action: #selector(GalleryGridCtrl.selectClicked))
        filterBtn = UIBarButtonItem(title: "Filter", style: .plain, target: self, action: #selector(GalleryGridCtrl.filterClicked))
        cancelBtn = UIBarButtonItem(title: "Cancel", style: .plain, target: self, action: #selector(GalleryGridCtrl.cancelClicked))
        navigationItem.rightBarButtonItems = [selectBtn!, filterBtn!]
        
        trashBtn.target = self
        trashBtn.action = #selector(GalleryGridCtrl.trashClicked)
        
        exportBtn.target = self
        exportBtn.action = #selector(GalleryGridCtrl.exportClicked)
    }
    
    override func viewWillAppear(_ animated: Bool)
    {
        refresh()
        
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    private func refresh()
    {
        let filter = Cookie.instance.galleryFilter
        let types = Cookie.instance.mediaTypesFilter
        (headers, files) = Files.instance.getGroupedGalleryFiles(by: filter, ofType: types)
        
        DispatchQueue.main.async {
        [unowned self] in
            self.collectionView.reloadData()
        }
    }
    
    @IBOutlet weak var gridLayout: GalleryGridLayout!
    
    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator)
    {
        var cells = collectionView.visibleCells
        let sz = cells.count
        var smallest:IndexPath? = nil
        for i in 0 ... (sz - 1)
        {
            let cell = cells[i]
            let indexPath = collectionView.indexPath(for: cell)
            
            if (indexPath != nil)
            {
                if (smallest == nil)
                    { smallest = indexPath }
                else
                {
                    let result = smallest!.compare(indexPath!)
                    if (result == .orderedDescending)
                        { smallest = indexPath }
                }
            }
        }
        
        coordinator.animate(alongsideTransition: nil, completion:
        { [unowned self, smallest] (context: UIViewControllerTransitionCoordinatorContext)  in
            if (smallest == nil)
                { return }
            
            self.collectionView.scrollToItem(at: smallest!, at: UICollectionViewScrollPosition.top, animated: false)
        })
    }
    
    @objc func trashClicked()
    {
        if (selectedCells.count == 0)
            { return }
        
        var indices = [PHAsset]()
        for cell in selectedCells
        {
            indices.append(files[cell.section][cell.item])
        }
        
        let result = Files.instance.deleteAssets(indices)
        
        if (result == true)
        {
            selectedCells.removeAll()
            refresh()
        }
    }
    
    private lazy var exporter = MediaExporter(ctrl: self)
    
    @objc func exportClicked()
    {
        if (selectedCells.count == 0)
            { return }
        
        var assets = [PHAsset]()
        for cell in selectedCells
        {
            let file = files[cell.section][cell.item]
            assets.append(file)
        }
        
        exporter.exportAction(files: assets)
    }
    
    private var isSelecting = false
    private var selectedCells = Set<IndexPath>()
    
    @objc func cancelClicked()
    {
        isSelecting = false
        
        for indexPath in selectedCells
        {
            let cell = collectionView.cellForItem(at: indexPath)
            
            if (cell != nil)
            {
                let widget = cell as! GalleryGridWidget
                widget.isSelected = false
            }
        }
        
        selectedCells.removeAll()
        
        bottomToolbar.isHidden = true
        navigationItem.rightBarButtonItems = [selectBtn!, filterBtn!]
    }
    
    @objc func selectClicked() {
        isSelecting = true
        bottomToolbar.isHidden = false
        
        navigationItem.rightBarButtonItems = [cancelBtn!]
    }
    
    func numberOfSections(in collectionView: UICollectionView) -> Int
    {
        return headers.count
    }
    
    @objc func filterClicked() {
        guard let vc = storyboard?.instantiateViewController(withIdentifier: "filterSelectCtrl") else { return }
        
        vc.modalPresentationStyle = .popover
        vc.popoverPresentationController?.barButtonItem = filterBtn
        present(vc, animated: true)
        
        guard let vc2 = vc as? FilterSelectCtrl else { return }
        vc2.onDismissed = { [weak self] in
            self?.refresh()
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int
    {
        return files[section].count
    }
    
    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView
    {
        let view = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier: "galleryGridHeader", for: indexPath)
        guard (view is GalleryGridHeader) else { return view }
        
        let view2 = view as! GalleryGridHeader
        
        let header = headers[indexPath.section]
        view2.title = header
        
        return view2
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell
    {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "galleryGridWidget", for: indexPath)
        guard let cell2 = cell as? GalleryGridWidget else { return cell }
        
        let file:PHAsset = files[indexPath.section][indexPath.item]
        cell2.displayContent(asset: file)
        
        let selected = selectedCells.contains(indexPath)
        cell2.isSelected = selected

        return cell2
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath)
    {
        if (!isSelecting)
        {
            var index = 0
            if (indexPath.section > 0)
            {
                for i in 0 ... (indexPath.section - 1)
                {
                    index += files[i].count
                }
            }
            
            index += indexPath.item
        
            performSegue(withIdentifier: "GalleryToPlayback", sender: index)
            return
        }
        
        
        let cell = collectionView.cellForItem(at: indexPath)
        let widget = cell as! GalleryGridWidget
        let selected = (selectedCells.contains(indexPath)) ? false : true
        
        widget.isSelected = selected
        
        if (selected)
            { selectedCells.insert(indexPath) }
        else
            { selectedCells.remove(indexPath) }
        
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

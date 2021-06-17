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
        refresh()
        
        navigationController?.setNavigationBarHidden(false, animated: animated)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        navigationController?.setNavigationBarHidden(true, animated: animated)
    }
    
    private func refresh()
    {
        (headers, files) = Files.instance.getGroupedGalleryFiles()
        
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
    
    private let SELECTION_COPY = "Copy"
    private let SELECTION_ROTATE_INSTA = "Rotate for Instagram"
    private let SELECTION_SQUARE = "Square aspect"
    private let SELECTION_SQUARE_ROTATED = "Square aspect - rotated"
    
    @objc func exportClicked()
    {
        if (selectedCells.count == 0)
            { return }
        
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
        for cell in selectedCells
        {
            let file = files[cell.section][cell.item]
            Files.instance.assetToImage(file, completed: {img in
                guard let image = img else { return }
                toShare.append( ImageProvider(placeholderItem: image, type: type ))
            })
        }
        
        let shareCtrl = UIActivityViewController(activityItems: toShare, applicationActivities: nil)
        present(shareCtrl, animated: true, completion: nil)
    }
    
    private var isSelecting = false
    private var selectedCells = Set<IndexPath>()
    
    @objc func selectClicked()
    {
        if (isSelecting)
        {
            isSelecting = false
            selectBtn?.title = "Select"
            navigationItem.title = "Gallery"
            
            for indexPath in selectedCells
            {
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
    
    func numberOfSections(in collectionView: UICollectionView) -> Int
    {
        return headers.count
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
        cell2.isHighlighted = selected

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
        
        widget.isHighlighted = selected
        
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

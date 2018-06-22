import UIKit

class GalleryGridCtrl: UIViewController, UICollectionViewDelegate, UICollectionViewDataSource
{
    @IBOutlet weak var collectionView: UICollectionView!
    
    private var dataDir = Files.getDataDir()
    private var files = [URL]()
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        do
        {
            files = try FileManager.default.contentsOfDirectory(at: dataDir!, includingPropertiesForKeys: nil)
        }
        catch {
            print("failed to get a listing of saved files")
        }
    }
    
    override func didReceiveMemoryWarning()
    {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
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
        
        let file = files[indexPath.item]
        
        do
        {
            let data = try Data(contentsOf: file)
            let image = UIImage(data: data)
            
            if (image != nil)
                { cell2.displayContent(image: image!) }
        }
        catch{
            print ("failed to load image at " + file.path)
        }
        
        return cell2
    }
}

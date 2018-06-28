//
//  ConnectViewController.swift

//  stereoCamera
//
//  Created by hallmarklabs on 6/13/18.
//  Copyright © 2018 cody. All rights reserved.
//

import UIKit

class ConnectCtrl: UIViewController
{
    @IBOutlet weak var layout: UIStackView!
    @IBOutlet weak var connectSecondaryBtn: UIButton!
    @IBOutlet weak var connectPrimaryInput: UITextField!
    @IBOutlet weak var connectPrimaryBtn: UIButton!
    
    let listenTimeout = 60.0
    let connectTimeout = 2.5
    
    var addressGuess = CommManager.instance.guessAddress()
    let alert = UIAlertController(title: nil, message: "Listening", preferredStyle: UIAlertControllerStyle.alert)
    
    func showLoader(_ show:Bool)
    {
        if (show)
        {
            present(alert, animated: true, completion: nil)
        }
        else
        {
            dismiss(animated: false, completion: nil)
        }
    }
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        setupLoader()
        setupLabels()
        firstConnect()
    }
    
    func setupLabels()
    {
        let commMgr = CommManager.instance
        for address in commMgr.localAddresses
        {
            let label = UILabel()
            label.text = address
            layout.insertArrangedSubview(label, at: 1)
        }
        
        if (!addressGuess.isMaster)
        {
            connectPrimaryInput.text = addressGuess.address
        }
    }
    
    func setupLoader()
    {
        let loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 10, y: 5, width: 50, height: 50 ))
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.gray
        loadingIndicator.startAnimating()
    
        alert.view.addSubview(loadingIndicator)
        
        let action = UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel, handler: { (action) in
            CommManager.instance.comm.disconnect()
            
            self.dismiss(animated: true, completion: nil)
        })
        alert.addAction(action)
    }
    
    func firstConnect()
    {
        var timeout = listenTimeout
        if (!addressGuess.isMaster)
        {
            timeout = connectTimeout
            alert.message = "Connecting to " + addressGuess.address
        }
        
        present(alert, animated: true, completion: nil)
        
        CommManager.instance.comm.connect(
            onConnected: onConnected,
            onFail: onFail,
            timeout: timeout
        )
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    func onConnected()
    {
        if (CommManager.instance.isMaster)
        {
            onMasterConnected()
        }
        else
        {
            onSlaveConnected()
        }
    }
    
    func onMasterConnected()
    {
        DispatchQueue.main.async
        {[unowned self] in
            self.dismiss(animated: true, completion: {
            [unowned self] in
                self.performSegue(withIdentifier: "CameraMasterSegue", sender: self)
            })
        }
        
    }
    
    func onSlaveConnected()
    {
        dismiss(animated: true, completion: {
        [unowned self] in
            self.performSegue(withIdentifier: "ConnectToSlave", sender: self)
        })
    }
    
    func onFail()
    {
        self.dismiss(animated: true, completion: nil)
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */
    @IBAction func listenSecondaryAction(_ sender: Any)
    {
        alert.message = "Listening for connections"
        present(alert, animated: true, completion: nil)
        
        CommManager.instance.comm.connect(master: true, address: "", onConnected: onConnected, onFail: onFail, timeout: listenTimeout)
    }
    
    @IBAction func connectPrimaryAction(_ sender: Any)
    {
        var address = connectPrimaryInput.text
        
        if (address == nil || address == "")
            { address = addressGuess.address }
        
        alert.message = "Connecting to: " + address!
        present(alert, animated: true, completion: nil)
        
        CommManager.instance.comm.connect(master: false, address: address!, onConnected: onConnected, onFail: onFail, timeout: connectTimeout)
    }
}

//
//  ConnectViewController.swift

//  stereoCamera
//
//  Created by hallmarklabs on 6/13/18.
//  Copyright © 2018 cody. All rights reserved.
//

import UIKit

class ConnectCtrl: UIViewController, UITextFieldDelegate
{
    @IBOutlet weak var layout: UIStackView!
    @IBOutlet weak var connectSecondaryBtn: UIButton!
    @IBOutlet weak var connectPrimaryInput: UITextField!
    @IBOutlet weak var connectPrimaryBtn: UIButton!
    @IBOutlet weak var galleryBtn: GalleryBtn!
    @IBOutlet weak var scrollView: UIScrollView!
    
    private let listenTimeout = 60.0
    private let connectTimeout = 2.5
    
    private var addressGuess = CommManager.instance.guessAddress()
    
    func showLoader(_ show:Bool, message:String = "Loading ...")
    {
        if (show)
        {
            let ctrl = LoadingPopupCtrl.initFromStoryboard()
            
            ctrl.header = message
            ctrl.addAction(action: LoadingPopupCtrl.Action(text: "Cancel", onClick: connectCancelled))
            
            present(ctrl, animated: true, completion: nil)
        }
        else
        {
            dismiss(animated: false, completion: nil)
        }
    }
    
    func connectCancelled(ctrl:LoadingPopupCtrl, _:LoadingPopupCtrl.Action)
    {
        showLoader(false)
        CommManager.instance.comm.disconnect()
    }
    
    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        setupLabels()
        firstConnect()
        
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: NSNotification.Name.UIKeyboardWillShow, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: NSNotification.Name.UIKeyboardWillHide, object: nil)
        connectPrimaryInput.delegate = self
        connectPrimaryInput.addDoneButtonToKeyboard(target: self, myAction: #selector(inputReturn))
        
        galleryBtn.setNavigationController(ctrl: navigationController)
    }
    
    var keyboardIsPresent = false
    
    @objc func keyboardWillShow(notification:Notification)
    {
        if (!keyboardIsPresent)
        {
            let szValue = notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue
            let sz = szValue?.cgRectValue
            
            if (sz != nil)
            {
                scrollView.contentOffset.y += sz!.height - 100
                keyboardIsPresent = true
            }
        }
    }
    
    @objc func keyboardWillHide(notification:Notification)
    {
        if (keyboardIsPresent)
        {
            let szValue = notification.userInfo?[UIKeyboardFrameBeginUserInfoKey] as? NSValue
            let sz = szValue?.cgRectValue
            
            if (sz != nil)
            {
                scrollView.contentOffset.y = 0
                keyboardIsPresent = false
            }
        }
    }
    
    @objc func inputReturn()
    {
        let value = connectPrimaryInput.text
        
        if (value == nil || value?.count == 0)
            { return }
        
        let isValid = validateIP(value!)
        
        
        if (isValid)
            { connectPrimaryInput.resignFirstResponder() }
        else
        {
            let msg = "please enter a valid IP address"
            let alert = UIAlertController(title: nil, message: msg, preferredStyle: UIAlertControllerStyle.alert)
            present(alert, animated: true, completion: {
            [] in
                usleep(2000000)
                alert.dismiss(animated: true, completion: nil)
            })
        }
    }
    
    func validateIP(_ ip:String) -> Bool
    {
        let parts = ip.split(separator: ".")
        
        if (parts.count != 4)
            { return false }
        
        for part in parts
        {
            let partInt = Int(part)
            if (partInt == nil || partInt! < 0 || partInt! >= 256)
                { return false }
        }
        
        return true
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
            if (Cookie.instance.client != "")
                { connectPrimaryInput.text = Cookie.instance.client }
            else
                { connectPrimaryInput.text = addressGuess.address }
        }
    }
    
    func firstConnect()
    {
        var master = false
        var address = addressGuess.address
        var timeout = connectTimeout
        var message = "Connecting to " + address
        
        if (Cookie.instance.master || addressGuess.isMaster)
        {
            master = true
            address = ""
            timeout = listenTimeout
            message = "Listening for connections"
        }
        else if (Cookie.instance.client != "")
        {
            address = Cookie.instance.client
        }
        
        showLoader(true, message: message)
        
        CommManager.instance.comm.connect(
            master: master, address: address,
            onConnected: onConnected,
            onFail: onFail,
            timeout: timeout
        )
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    func onConnected(client:String)
    {
        if (CommManager.instance.isMaster)
        {
            onMasterConnected()
        }
        else
        {
            onSlaveConnected(client: client)
        }
    }
    
    func onMasterConnected()
    {
        DispatchQueue.main.async
        {[unowned self] in
            self.dismiss(animated: true, completion: {
            [unowned self] in
                Cookie.instance.master = true
                Cookie.instance.client = ""
                self.performSegue(withIdentifier: "CameraMasterSegue", sender: self)
            })
        }
        
    }
    
    func onSlaveConnected(client: String)
    {
        DispatchQueue.main.async
        {[unowned self] in
            self.dismiss(animated: true, completion: {
            [unowned self] in
                Cookie.instance.master = false
                Cookie.instance.client = client
                self.performSegue(withIdentifier: "ConnectToSlave", sender: self)
            })
        }
    }
    
    func onFail()
    {
        DispatchQueue.main.async {
        [unowned self] in
            self.dismiss(animated: true, completion: nil)
            CommManager.instance.comm.disconnect()
        }
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
        showLoader(true, message: "Listening")
        CommManager.instance.comm.connect(master: true, address: "", onConnected: onConnected, onFail: onFail, timeout: listenTimeout)
    }
    
    @IBAction func connectPrimaryAction(_ sender: Any)
    {
        var address = connectPrimaryInput.text
        
        if (address == nil || address == "")
            { address = addressGuess.address }
        
        showLoader(true, message: "Connecting to: " + address!)
        CommManager.instance.comm.connect(master: false, address: address!, onConnected: onConnected, onFail: onFail, timeout: connectTimeout)
    }
}

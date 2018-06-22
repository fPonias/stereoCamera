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

    override func viewDidLoad()
    {
        super.viewDidLoad()
        
        onMasterConnected2()
        
        lbl_reconnect.isHidden = true
        btn_primaryReconnect.isHidden = true
        btn_secondaryReconnect.isHidden = true
        
        CommManager.instance.comm.connect(
            onConnected: onConnected,
            onFail: {() -> Void in
            
            }
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
        let ping = Ping()
        
        CommManager.instance.comm.sendCommand(command: ping, listener: { (origCmd:Command?, respCmd:Command) -> Void in
            print("ping received, connection established")
            
            self.onMasterConnected2()
        })
    }
    
    func onMasterConnected2()
    {
        performSegue(withIdentifier: "CameraMasterSegue", sender: self)
    }
    
    func onSlaveConnected()
    {
        CommManager.instance.comm.addCommandListener(weak: { (command: Command) -> Void in
            switch (command.cmdtype)
            {
            case CommandTypes.PING:
                if (!command.isResponse)
                {
                    let ping = Ping()
                    ping.id = command.id
                    ping.isResponse = true
                    CommManager.instance.comm.sendCommand(command: ping)
                }
            default:
                print("unsupported command " + command.cmdtype.description)
            }
        })
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

    
    @IBAction func evt_primaryReconnect(_ sender: Any)
    {
    }
    
    @IBAction func evt_secondaryReconnect(_ sender: Any)
    {
    }
    
    @IBAction func evt_primaryDiscover(_ sender: Any)
    {
    }
    
    @IBAction func evt_secondaryDiscover(_ sender: Any) {
    }
    
    @IBOutlet weak var lbl_reconnect: UILabel!
    @IBOutlet weak var btn_primaryReconnect: UIButton!
    @IBOutlet weak var btn_secondaryReconnect: UIButton!
    @IBOutlet weak var btn_primaryDiscover: UIButton!
    @IBOutlet weak var btn_secondaryDiscover: UIButton!
}

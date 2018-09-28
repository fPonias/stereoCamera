package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class SendConnectionPause extends SlaveCommand
{
	public SendConnectionPause()
	{
		this.id = -1;
		this.command = BluetoothCommands.RECEIVE_CONNECTION_PAUSE;
	}
}

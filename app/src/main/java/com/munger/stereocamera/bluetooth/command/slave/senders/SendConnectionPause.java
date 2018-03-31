package com.munger.stereocamera.bluetooth.command.slave.senders;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

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

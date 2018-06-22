package com.munger.stereocamera.bluetooth.command.slave.senders;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class SendDisconnect extends SlaveCommand
{
	public SendDisconnect()
	{
		this.id = -1;
		this.command = BluetoothCommands.RECEIVE_DISCONNECT;
	}
}

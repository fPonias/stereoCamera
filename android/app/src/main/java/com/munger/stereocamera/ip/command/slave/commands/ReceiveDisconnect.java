package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class ReceiveDisconnect extends SlaveCommand
{
	public ReceiveDisconnect(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.DISCONNECT;
	}
}

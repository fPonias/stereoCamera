package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ReceiveConnectionPause extends SlaveCommand
{
	public ReceiveConnectionPause(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.CONNECTION_PAUSE;
	}
}

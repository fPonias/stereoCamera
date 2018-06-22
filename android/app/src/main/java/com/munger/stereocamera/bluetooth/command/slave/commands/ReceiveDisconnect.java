package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

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

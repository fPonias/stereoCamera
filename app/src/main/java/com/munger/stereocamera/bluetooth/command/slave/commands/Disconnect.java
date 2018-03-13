package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class Disconnect extends SlaveCommand
{
	public Disconnect(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.DISCONNECT;
	}
}

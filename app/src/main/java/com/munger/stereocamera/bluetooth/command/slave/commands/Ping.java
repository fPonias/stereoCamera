package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class Ping extends SlaveCommand
{
	public Ping(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.PING;
	}
}
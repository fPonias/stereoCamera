package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

public class Handshake extends SlaveCommand
{
	public Handshake (int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.HANDSHAKE;
	}
}

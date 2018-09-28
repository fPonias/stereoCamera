package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;

public class Handshake extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.HANDSHAKE;
	}
}

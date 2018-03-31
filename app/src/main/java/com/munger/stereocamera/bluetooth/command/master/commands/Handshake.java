package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

public class Handshake extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.HANDSHAKE;
	}
}

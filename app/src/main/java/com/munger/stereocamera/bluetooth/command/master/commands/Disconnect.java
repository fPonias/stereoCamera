package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class Disconnect extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.DISCONNECT;
	}

	@Override
	public void handleResponse() throws IOException
	{}

	@Override
	public void onExecuteFail()
	{}
}

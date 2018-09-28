package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ConnectionPause extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.CONNECTION_PAUSE;
	}

}

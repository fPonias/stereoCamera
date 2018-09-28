package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.master.MasterIncoming;
import com.munger.stereocamera.fragment.MasterFragment;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class Disconnect extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.DISCONNECT;
	}
}

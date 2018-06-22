package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
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

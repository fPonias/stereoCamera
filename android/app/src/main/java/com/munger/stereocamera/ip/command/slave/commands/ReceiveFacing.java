package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/7/18.
 */

public class ReceiveFacing extends SlaveCommand
{
	private boolean facing;

	public boolean getFacing()
	{
		return facing;
	}

	public ReceiveFacing(int i)
	{
		super();
		this.id = i;
		this.command = BluetoothCommands.SET_FACING;
	}

	@Override
	protected void readArguments() throws IOException
	{
		byte arg = comm.getByte();
		facing = (arg == 0) ? false : true;
	}
}

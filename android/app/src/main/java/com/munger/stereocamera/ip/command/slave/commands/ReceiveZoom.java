package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class ReceiveZoom extends SlaveCommand
{
	private float zoom;

	public float getZoom()
	{
		return zoom;
	}

	public ReceiveZoom(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.SET_ZOOM;
	}

	@Override
	protected void readArguments() throws IOException
	{
		zoom = comm.getFloat();
	}
}

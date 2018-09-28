package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;

import java.nio.ByteBuffer;

public class SetZoom extends MasterCommand
{
	private float zoom;

	public SetZoom(float zoom)
	{
		this.zoom = zoom;
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.SET_ZOOM;
	}

	@Override
	public byte[] getArguments()
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putFloat(zoom);

		return bb.array();
	}
}

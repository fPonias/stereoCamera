package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

import java.nio.ByteBuffer;

public class SetZoom extends MasterCommand
{
	private float zoom;

	public SetZoom(float zoom)
	{
		this.zoom = zoom;
	}

	@Override
	public Command.Type getCommand()
	{
		return Command.Type.SET_ZOOM;
	}

	@Override
	public byte[] getArguments()
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putFloat(zoom);

		return bb.array();
	}
}

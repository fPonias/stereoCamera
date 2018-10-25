package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Command;

public class SendZoom extends SetZoom
{
	public SendZoom()
	{
		super();
	}

	public SendZoom(float value)
	{
		super(value);
	}

	@Override
	protected void doInit()
	{
		super.doInit();
		cmdtype = Type.RECEIVE_ZOOM;
		expectsResponse = false;
	}
}

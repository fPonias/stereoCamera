package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SetZoom extends Command
{
	public float localZoom = 1.0f;
	public float remoteZoom = 1.0f;

	public SetZoom()
	{
		super();
		doInit();
	}

	public SetZoom(float localZoom, float remoteZoom)
	{
		super();
		this.localZoom = localZoom;
		this.remoteZoom = remoteZoom;
		doInit();
	}

	protected void doInit()
	{
		cmdtype = Type.SET_ZOOM;
		expectsResponse = true;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false;}

		try
		{
			comm.putFloat(localZoom);
			comm.putFloat(remoteZoom);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}

	@Override
	public boolean receive(Comm comm)
	{
		boolean result = super.receive(comm);
		if (!result)
			{ return false;}

		try
		{
			localZoom = comm.getFloat();
			remoteZoom = comm.getFloat();
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

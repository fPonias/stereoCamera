package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SetZoom extends Command
{
	public float zoom = 1.0f;

	public SetZoom()
	{
		super();
		doInit();
	}

	public SetZoom(float zoom)
	{
		super();
		this.zoom = zoom;
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
			comm.putFloat(zoom);
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
			zoom = comm.getFloat();
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SendGravity extends Command
{
	public static class Gravity
	{
		public float x;
		public float y;
		public float z;
	}

	public Gravity gravity;

	public SendGravity()
	{
		super();
		doInit();
	}

	public SendGravity(Gravity gravity)
	{
		super();
		this.gravity = gravity;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.RECEIVE_GRAVITY;
		expectsResponse = false;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false; }

		try
		{
			comm.putFloat(gravity.x);
			comm.putFloat(gravity.y);
			comm.putFloat(gravity.z);
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
			{ return false; }

		try
		{
			gravity = new Gravity();
			gravity.x = comm.getFloat();
			gravity.y = comm.getFloat();
			gravity.z = comm.getFloat();
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

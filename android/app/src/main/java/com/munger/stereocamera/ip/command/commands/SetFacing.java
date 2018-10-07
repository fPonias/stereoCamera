package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SetFacing extends Command
{
	public boolean facing = false;

	public SetFacing()
	{
		super();
		doInit();
	}

	public SetFacing(boolean facing)
	{
		super();
		this.facing = facing;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.SET_FACING;
		expectsResponse = true;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false; }

		try
		{
			comm.putByte((facing) ? (byte) 1 : (byte) 0);
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
			facing = comm.getBoolean();
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

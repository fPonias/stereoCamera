package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class ID extends Command
{
	public String phoneId = "";

	public ID()
	{
		super();
		doInit();
	}

	public ID(String phoneId)
	{
		super();
		this.phoneId = phoneId;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.ID;
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
			long sz = comm.getLong();

			if (sz == 0)
			{
				phoneId = "";
				return true;
			}

			byte[] data = comm.getData((int) sz);
			phoneId = new String(data);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SendPhoto extends Command
{
	public byte[] data;

	public SendPhoto()
	{
		super();
		doInit();
	}

	public SendPhoto(byte[] data)
	{
		super();
		this.data = data;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.SEND_PROCESSED_PHOTO;
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
			comm.putLong(data.length);

			if (data.length > 0)
				{ comm.putData(data); }
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
			long sz = comm.getLong();

			if (sz == 0)
			{
				data = new byte[0];
				return true;
			}

			data = comm.getData((int) sz);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

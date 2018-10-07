package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SendStatus extends Command
{
	public PreviewFragment.Status status;

	public SendStatus(PreviewFragment.Status status)
	{
		super();
		this.status = status;
		doInit();
	}

	public SendStatus()
	{
		super();
		this.status = PreviewFragment.Status.NONE;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.RECEIVE_STATUS;
		expectsResponse = false;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (result == false)
			{ return false; }

		try
		{
			comm.putByte((byte) status.ordinal());
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
		if (result == false)
			{ return false; }

		try
		{
			byte b = comm.getByte();
			PreviewFragment.Status[] statList = PreviewFragment.Status.values();

			if (b < 0 || b >= statList.length)
				return false;

			status = statList[b];

		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

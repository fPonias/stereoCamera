package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class GetRemoteLatency extends MasterCommand
{
	public GetRemoteLatency()
	{}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.LATENCY_CHECK;
	}

	@Override
	public MasterIncoming getResponse()
	{
		return new Response(id);
	}

	public static class Response extends MasterIncoming
	{
		public long latency;

		public Response(int id)
		{
			super(BluetoothCommands.LATENCY_CHECK, id);
		}

		@Override
		public void readResponse() throws IOException
		{
			latency = parent.readLong();
		}
	}
}

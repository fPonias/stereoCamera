package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class Ping extends MasterCommand
{
	@Override
	public Command.Type getCommand()
	{
		return Command.Type.PING;
	}

	private long then;

	@Override
	public void onStart()
	{
		then = System.currentTimeMillis();
	}

	@Override
	public MasterIncoming getResponse()
	{
		return new Response(id);
	}

	public class Response extends MasterIncoming
	{
		public long latency;

		public Response(int id)
		{
			super(Command.Type.PING, id);
		}

		@Override
		public void readResponse() throws IOException
		{
			long now = System.currentTimeMillis();
			latency = now - then;
		}
	}
}

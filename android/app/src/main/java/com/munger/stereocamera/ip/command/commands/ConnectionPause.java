package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Command;

public class ConnectionPause extends Command
{
	public ConnectionPause()
	{
		super();
		cmdtype = Type.CONNECTION_PAUSE;
		expectsResponse = false;
	}
}

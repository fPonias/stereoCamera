package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Command;

public class SendConnectionPause extends Command
{
	public SendConnectionPause()
	{
		super();
		cmdtype = Type.RECEIVE_CONNECTION_PAUSE;
		expectsResponse = false;
	}
}

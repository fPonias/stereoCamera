package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Command;

public class SendDisconnect extends Command
{
	public SendDisconnect()
	{
		super();
		cmdtype = Type.RECEIVE_DISCONNECT;
		expectsResponse = false;
	}
}

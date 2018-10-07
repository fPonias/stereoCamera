package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Command;

public class Disconnect extends Command
{
	public Disconnect()
	{
		super();
		cmdtype = Type.DISCONNECT;
		expectsResponse = false;
	}
}

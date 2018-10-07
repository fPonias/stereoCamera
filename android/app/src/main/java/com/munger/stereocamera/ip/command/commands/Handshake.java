package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Command;

public class Handshake extends Command
{
	public Handshake()
	{
		super();

		cmdtype = Type.HANDSHAKE;
		expectsResponse = true;
	}
}

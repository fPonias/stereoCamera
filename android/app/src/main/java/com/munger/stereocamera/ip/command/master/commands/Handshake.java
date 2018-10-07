package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

public class Handshake extends MasterCommand
{
	@Override
	public Command.Type getCommand()
	{
		return Command.Type.HANDSHAKE;
	}
}

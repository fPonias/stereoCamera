package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class Disconnect extends MasterCommand
{
	@Override
	public Command.Type getCommand()
	{
		return Command.Type.DISCONNECT;
	}
}

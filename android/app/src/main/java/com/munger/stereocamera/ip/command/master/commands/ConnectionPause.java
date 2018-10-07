package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ConnectionPause extends MasterCommand
{
	@Override
	public Command.Type getCommand()
	{
		return Command.Type.CONNECTION_PAUSE;
	}

}

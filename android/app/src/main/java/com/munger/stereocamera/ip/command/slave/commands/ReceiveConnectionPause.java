package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ReceiveConnectionPause extends SlaveCommand
{
	public ReceiveConnectionPause(int id)
	{
		super();
		this.id = id;
		this.command = Command.Type.CONNECTION_PAUSE;
	}
}

package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

public class Handshake extends SlaveCommand
{
	public Handshake (int id)
	{
		super();
		this.id = id;
		this.command = Command.Type.HANDSHAKE;
	}
}

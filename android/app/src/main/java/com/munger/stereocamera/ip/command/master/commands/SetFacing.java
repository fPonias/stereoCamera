package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

/**
 * Created by hallmarklabs on 3/7/18.
 */

public class SetFacing extends MasterCommand
{
	public SetFacing(boolean facing)
	{
		this.facing = facing;
	}

	@Override
	public Command.Type getCommand()
	{
		return Command.Type.SET_FACING;
	}

	private boolean facing;

	@Override
	public byte[] getArguments()
	{
		byte[] ret = new byte[1];
		ret[0] = (facing) ? (byte) 1 : (byte) 0;
		return ret;
	}
}

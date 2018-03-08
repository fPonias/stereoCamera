package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/7/18.
 */

public class SetFacing extends MasterCommand
{
	public SetFacing(boolean facing, Listener listener)
	{
		this.facing = facing;
		this.listener = listener;
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.SET_FACING;
	}

	public interface Listener
	{
		void done();
		void fail();
	}

	private Listener listener;
	private boolean facing;

	@Override
	public void onExecuteFail()
	{
		listener.fail();
	}

	@Override
	public void handleResponse() throws IOException
	{
		listener.done();
	}

	@Override
	public byte[] getArguments()
	{
		byte[] ret = new byte[1];
		ret[0] = (facing) ? (byte) 1 : (byte) 0;
		return ret;
	}
}

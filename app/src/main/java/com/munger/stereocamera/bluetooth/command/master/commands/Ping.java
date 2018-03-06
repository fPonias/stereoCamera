package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class Ping extends MasterCommand
{
	private Listener listener;

	public Ping(Listener listener)
	{
		this.listener = listener;
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.PING;
	}

	@Override
	public void onExecuteFail()
	{
		listener.fail();
	}

	private long then;

	@Override
	public void onStart()
	{
		then = System.currentTimeMillis();
	}

	@Override
	public void handleResponse() throws IOException
	{
		long now = System.currentTimeMillis();
		long diff = now - then;

		listener.pong(diff);
	}

	public interface Listener
	{
		void pong(long diff);
		void fail();
	}
}

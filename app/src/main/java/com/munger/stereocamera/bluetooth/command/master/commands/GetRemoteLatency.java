package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class GetRemoteLatency extends MasterCommand
{
	public Listener listener;

	public GetRemoteLatency(Listener listener)
	{
		this.listener = listener;
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.LATENCY_CHECK;
	}

	@Override
	public void onExecuteFail()
	{
		listener.fail();
	}

	@Override
	public void handleResponse() throws IOException
	{
		long ret = parent.readLong();
		listener.done(ret);
	}

	public interface Listener
	{
		void done(long latency);
		void fail();
	}
}

package com.munger.stereocamera.bluetooth.command.master.commands;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class Flip extends MasterCommand
{
	private Listener listener;

	public Flip(Listener listener)
	{
		this.listener = listener;
	}

	public interface Listener
	{
		void done();
		void fail();
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.FLIP;
	}

	@Override
	public void onExecuteFail()
	{
		listener.fail();
	}

	@Override
	public void handleResponse()
	{
		listener.done();
	}
}

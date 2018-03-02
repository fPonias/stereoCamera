package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

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

	public interface Listener
	{
		void pong(long diff);
		void fail();
	}

	public void execute()
	{
		this.listener = listener;

		try
		{
			ping2();
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.PING.name() + " success");
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.PING.name() + " failed");
			listener.fail();
		}
	}

	private void ping2() throws IOException
	{
		long now = System.currentTimeMillis();
		parent.sendCommand(BluetoothCommands.PING);

		long then = System.currentTimeMillis();
		long diff = then - now;

		listener.pong(diff);
	}
}

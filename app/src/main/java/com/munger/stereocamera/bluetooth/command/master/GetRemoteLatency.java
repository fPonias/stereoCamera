package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

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

	public interface Listener
	{
		void done(long latency);
		void fail();
	}

	@Override
	protected void execute()
	{
		long ret;
		try
		{
			parent.sendCommand(BluetoothCommands.LATENCY_CHECK);
			ret = parent.readLong();
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " remote check failed");
			listener.fail();
			return;
		}

		Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " remote check success");
		listener.done(ret);
	}
}

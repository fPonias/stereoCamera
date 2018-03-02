package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class GetStatus extends MasterCommand
{
	private Listener listener;

	public GetStatus(Listener listener)
	{
		this.listener = listener;
	}

	public interface Listener
	{
		void done(int status);
		void fail();
	}

	public void execute()
	{
		try
		{
			parent.sendCommand(BluetoothCommands.GET_STATUS);
			int ret = parent.readInt();

			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_STATUS.name() + " success");
			listener.done(ret);
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_STATUS.name() + " failed");
			listener.fail();
		}
	}
}

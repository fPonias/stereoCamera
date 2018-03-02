package com.munger.stereocamera.bluetooth.command.master;

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

	public void execute()
	{
		try
		{
			parent.sendCommand(BluetoothCommands.FLIP);

			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FLIP.name() + " success");
			listener.done();
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FLIP.name() + " failed");
			listener.fail();
		}
	}
}

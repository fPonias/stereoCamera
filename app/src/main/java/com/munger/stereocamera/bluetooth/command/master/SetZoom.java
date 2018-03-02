package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class SetZoom extends MasterCommand
{
	private Listener listener;
	private float zoom;

	public SetZoom(float zoom, Listener listener)
	{
		this.zoom = zoom;
		this.listener = listener;
	}

	public interface Listener
	{
		void done();
		void fail();
	}

	public void execute()
	{
		byte[] args = ByteBuffer.allocate(4).putFloat(zoom).array();
		try
		{
			parent.sendCommand(BluetoothCommands.SET_ZOOM, args);
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.SET_ZOOM.name() + " success");
			listener.done();
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.SET_ZOOM.name() + " failed");
			listener.fail();
		}
	}
}

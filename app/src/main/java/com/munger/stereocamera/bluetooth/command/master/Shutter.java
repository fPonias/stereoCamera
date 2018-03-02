package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class Shutter extends MasterCommand
{
	private Listener listener;

	public Shutter(Listener listener)
	{
		this.listener = listener;
	}

	public interface Listener
	{
		void onData(byte[] data);
		void fail();
	}

	public void execute()
	{
		try
		{
			shutter2();
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FIRE_SHUTTER.name() + " success");
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FIRE_SHUTTER.name() + " failed");
			listener.fail();
		}
	}

	private void shutter2() throws IOException
	{
		parent.sendCommand(BluetoothCommands.FIRE_SHUTTER);

		int size = parent.readInt();
		ByteBuffer imgBuffer = ByteBuffer.allocate(size);
		int sz = 0;

		int read;
		while (sz < size)
		{
			read = parent.ins.read(parent.buffer);
			sz += read;
			imgBuffer.put(parent.buffer, 0 , read);
		}

		listener.onData(imgBuffer.array());
	}
}

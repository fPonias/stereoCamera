package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientations;

import java.io.IOException;

public class Shutter extends MasterCommand
{
	private Listener listener;

	public Shutter(Listener listener)
	{
		this.listener = listener;
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.FIRE_SHUTTER;
	}

	@Override
	public void onExecuteFail()
	{
		listener.fail();
	}

	@Override
	public void handleResponse() throws IOException
	{
		PhotoOrientations orientation = PhotoOrientations.values()[parent.readInt()];
		float zoom = parent.readFloat();

		int size = parent.readInt();
		byte[] imgBuffer = new byte[size];
		int sz = 0;

		int read;
		while (sz < size)
		{
			read = parent.ins.read(imgBuffer, sz, size - sz);
			sz += read;
		}

		listener.onData(orientation, zoom, imgBuffer);
	}

	public interface Listener
	{
		void onData(PhotoOrientations orientation, float zoom, byte[] data);
		void fail();
	}
}

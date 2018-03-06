package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

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

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.SET_ZOOM;
	}

	@Override
	public byte[] getArguments()
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putFloat(zoom);

		return bb.array();
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

	public interface Listener
	{
		void done();
		void fail();
	}
}

package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class ReceiveZoom extends MasterListener
{
	public interface Listener
	{
		void done(float zoom);
		void fail();
	}

	private Listener listener;

	public ReceiveZoom(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_ZOOM);
		this.listener = listener;
	}

	@Override
	public void handleResponse() throws IOException
	{
		float value = parent.readFloat();
		listener.done(value);
	}

	@Override
	public void onFail()
	{
		listener.fail();
	}
}

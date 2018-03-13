package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ReceiveDisconnect extends MasterListener
{
	public interface Listener
	{
		void received();
	}

	private Listener listener;

	public ReceiveDisconnect(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_DISCONNECT);

		this.listener = listener;
	}

	@Override
	public void handleResponse() throws IOException
	{
		listener.received();
	}

	@Override
	public void onFail()
	{
		listener.received();
	}
}

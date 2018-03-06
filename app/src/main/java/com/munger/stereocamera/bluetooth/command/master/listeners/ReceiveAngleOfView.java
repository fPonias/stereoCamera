package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

public class ReceiveAngleOfView extends MasterListener
{
	public ReceiveAngleOfView(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_ANGLE_OF_VIEW);
		this.listener = listener;
	}

	private Listener listener;

	public interface Listener
	{
		void done(float horiz, float vert);
		void fail();
	}

	@Override
	public void handleResponse() throws IOException
	{
		float[] ret = new float[2];

		for (int i = 0; i < 2; i++)
		{
			ret[i] = parent.readFloat();
		}

		listener.done(ret[0], ret[1]);
	}

	@Override
	public void onFail()
	{
		listener.fail();
	}
}

package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;


public class ReceiveGravity extends MasterListener
{
	private Listener listener;

	public ReceiveGravity(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_GRAVITY);
		this.listener = listener;
	}

	@Override
	public void handleResponse() throws IOException
	{
		Gravity ret = new Gravity();
		ret.x = parent.readFloat();
		ret.y = parent.readFloat();
		ret.z = parent.readFloat();

		listener.done(ret);
	}

	@Override
	public void onFail()
	{
		listener.fail();
	}

	public static class Gravity
	{
		public float x;
		public float y;
		public float z;
	}

	public interface Listener
	{
		void done(Gravity value);
		void fail();
	}
}

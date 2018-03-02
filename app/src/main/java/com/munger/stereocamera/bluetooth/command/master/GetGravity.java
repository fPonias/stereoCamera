package com.munger.stereocamera.bluetooth.command.master;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class GetGravity extends MasterCommand
{
	private Listener listener;

	public GetGravity(Listener listener)
	{
		this.listener = listener;
	}

	public class Gravity
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

	public void execute()
	{
		try
		{
			parent.sendCommand(BluetoothCommands.GET_GRAVITY);
			Gravity ret = new Gravity();
			ret.x = parent.readFloat();
			ret.y = parent.readFloat();
			ret.z = parent.readFloat();

			listener.done(ret);
		}
		catch(IOException e){
			listener.fail();
		}
	}
}

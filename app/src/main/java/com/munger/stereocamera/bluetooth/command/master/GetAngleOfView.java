package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;

public class GetAngleOfView extends MasterCommand
{
	private Listener listener;

	public GetAngleOfView(Listener listener)
	{
		this.listener = listener;
	}


	public interface Listener
	{
		void done(float horiz, float vert);
		void fail();
	}

	public void execute()
	{
		float[] ret = new float[2];
		try
		{
			parent.sendCommand(BluetoothCommands.GET_ANGLE_OF_VIEW);

			for (int i = 0; i < 2; i++)
			{
				ret[i] = parent.readFloat();
			}

			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_ANGLE_OF_VIEW.name() + " success");
			listener.done(ret[0], ret[1]);
		}
		catch(IOException e){
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_ANGLE_OF_VIEW.name() + " failed");
			listener.fail();
		}
	}
}

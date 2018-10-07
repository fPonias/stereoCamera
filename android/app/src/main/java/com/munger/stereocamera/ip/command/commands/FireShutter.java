package com.munger.stereocamera.ip.command.commands;

import android.util.Log;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class FireShutter extends Command
{
	public byte[] data;
	public float zoom;

	public FireShutter()
	{
		super();
		doInit();
	}

	public FireShutter(byte[] data, float zoom)
	{
		super();
		this.data = data;
		this.zoom = zoom;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.FIRE_SHUTTER;
		expectsResponse = true;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false; }

		try
		{
			comm.putFloat(zoom);
			comm.putLong(data.length);
			if (data.length > 0)
			{
				comm.putData(data);
			}
		}
		catch(IOException e){
			return false;
		}

		return true;
	}

	@Override
	public boolean receive(Comm comm)
	{
		boolean result = super.receive(comm);
		if (!result)
			{ return false; }

		try
		{
			zoom = comm.getFloat();
			Log.d("StereoCamera", "read zoom value: " + zoom);

			long sz = comm.getLong();
			Log.d("StereoCamera", "read size value: " + sz);

			if (sz == 0)
			{
				data = new byte[0];
				return true;
			}

			data = comm.getData((int) sz);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

package com.munger.stereocamera.ip.command.commands;

import android.util.Log;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;

import java.io.IOException;

public class FireShutter extends Command
{


	public byte[] data;
	public float zoom;
	public PhotoOrientation orientation;

	public FireShutter()
	{
		super();

		zoom = 1.0f;
		orientation = PhotoOrientation.DEG_0;

		doInit();
	}

	public FireShutter(byte[] data, float zoom, PhotoOrientation orientation)
	{
		super();
		this.data = data;
		this.zoom = zoom;
		this.orientation = orientation;
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
			comm.putByte((byte) orientation.ordinal());

			if (data != null)
			{
				comm.putLong(data.length);
				if (data.length > 0)
				{
					comm.putData(data);
				}
			}
			else
				comm.putLong(0);
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
			Log.d("stereoCamera", "read zoom value: " + zoom);

			byte orient = comm.getByte();
			Log.d("stereoCamera", "read orientation value: " + orient);
			orientation = PhotoOrientation.values()[orient];
			
			long sz = comm.getLong();
			Log.d("stereoCamera", "read size value: " + sz);

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

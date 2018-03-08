package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class Shutter extends SlaveCommand
{
	public Shutter(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.FIRE_SHUTTER;
	}

	private byte[] imgData;
	private PhotoOrientation orientation = PhotoOrientation.DEG_0;
	private float zoom = 1.0f;

	public void setData(byte[] data)
	{
		imgData = data;
	}
	public void setOrientation(PhotoOrientation orientation)
	{
		this.orientation = orientation;
	}
	public void setZoom(float zoom)
	{
		this.zoom = zoom;
	}

	@Override
	public void send() throws IOException
	{
		comm.putInt(orientation.ordinal());
		comm.putFloat(zoom);

		if (imgData != null)
		{
			comm.putInt(imgData.length);
			comm.outs.write(imgData);
		}
		else
		{
			comm.putInt(0);
		}

		comm.outs.flush();
	}
}

package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

public class AudioSyncStart extends SlaveCommand
{
	public AudioSyncStart(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.AUDIO_SYNC_START;
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
			comm.putData(imgData);
		}
		else
		{
			comm.putInt(0);
		}
	}
}

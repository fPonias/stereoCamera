package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class SendZoom extends SlaveCommand
{
	private float zoom;

	public SendZoom(float value)
	{
		this.id = -1;
		this.command = Command.Type.RECEIVE_ZOOM;
		this.zoom = value;
	}

	@Override
	public void send() throws IOException
	{
		comm.putFloat(zoom);
	}
}

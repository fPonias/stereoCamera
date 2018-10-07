package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

public class SendPreviewFrame extends SlaveCommand
{
	private byte[] data; //jpeg encoded data
	private float zoom;

	public SendPreviewFrame(byte[] data, float zoom)
	{
		this.id = -1;
		this.command = Command.Type.RECEIVE_PREVIEW_FRAME;
		this.data = data;
		this.zoom = zoom;
	}

	@Override
	public void send() throws IOException
	{
		comm.putFloat(zoom);
		comm.putInt(data.length);
		comm.putData(data);
	}
}

package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

public class SendOrientation extends SlaveCommand
{
	private PhotoOrientation orientation;

	public SendOrientation(PhotoOrientation orientation)
	{
		this.id = -1;
		this.command = Command.Type.RECEIVE_ORIENTATION;
		this.orientation = orientation;
	}

	@Override
	public void send() throws IOException
	{
		comm.putByte((byte) orientation.ordinal());
	}
}

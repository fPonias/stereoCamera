package com.munger.stereocamera.bluetooth.command.slave.senders;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

public class SendOrientation extends SlaveCommand
{
	private PhotoOrientation orientation;

	public SendOrientation(PhotoOrientation orientation)
	{
		this.id = -1;
		this.command = BluetoothCommands.RECEIVE_ORIENTATION;
		this.orientation = orientation;
	}

	@Override
	public void send() throws IOException
	{
		comm.outs.write(orientation.ordinal());
	}
}

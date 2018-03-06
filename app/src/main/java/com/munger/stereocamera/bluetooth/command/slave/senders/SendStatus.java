package com.munger.stereocamera.bluetooth.command.slave.senders;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class SendStatus extends SlaveCommand
{
	private int status;

	public SendStatus(int status)
	{
		this.id = -1;
		this.command = BluetoothCommands.RECEIVE_STATUS;
		this.status = status;
	}

	@Override
	public void send() throws IOException
	{
		comm.putInt(status);
	}
}

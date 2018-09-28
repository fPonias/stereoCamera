package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class SendAngleOfView extends SlaveCommand
{
	private float vert;
	private float horiz;

	public SendAngleOfView(float vert, float horiz)
	{
		this.id = -1;
		this.command = BluetoothCommands.RECEIVE_ANGLE_OF_VIEW;
		this.vert = vert;
		this.horiz = horiz;
	}

	@Override
	public void send() throws IOException
	{
		comm.putFloat(horiz);
		comm.putFloat(vert);
	}
}

package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class SendGravity extends SlaveCommand
{
	private ReceiveGravity.Gravity gravity;

	public SendGravity(ReceiveGravity.Gravity value)
	{
		this.id = -1;
		this.command = Command.Type.RECEIVE_GRAVITY;
		this.gravity = value;
	}

	@Override
	public void send() throws IOException
	{
		comm.putFloat(gravity.x);
		comm.putFloat(gravity.y);
		comm.putFloat(gravity.z);
	}
}

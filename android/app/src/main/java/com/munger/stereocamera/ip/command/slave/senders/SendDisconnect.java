package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class SendDisconnect extends SlaveCommand
{
	public SendDisconnect()
	{
		this.id = -1;
		this.command = Command.Type.RECEIVE_DISCONNECT;
	}
}

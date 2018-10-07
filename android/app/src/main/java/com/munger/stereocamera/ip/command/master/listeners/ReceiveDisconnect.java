package com.munger.stereocamera.ip.command.master.listeners;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class ReceiveDisconnect extends MasterIncoming
{
	public ReceiveDisconnect()
	{
		super(Command.Type.RECEIVE_DISCONNECT, -1);
	}
}

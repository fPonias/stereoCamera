package com.munger.stereocamera.ip.command.master.listeners;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ReceiveConnectionPause extends MasterIncoming
{
	public ReceiveConnectionPause()
	{
		super(Command.Type.RECEIVE_CONNECTION_PAUSE, -1);
	}
}

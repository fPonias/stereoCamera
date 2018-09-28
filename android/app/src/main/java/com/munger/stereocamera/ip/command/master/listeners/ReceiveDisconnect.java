package com.munger.stereocamera.ip.command.master.listeners;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/28/18.
 */

public class ReceiveDisconnect extends MasterIncoming
{
	public ReceiveDisconnect()
	{
		super(BluetoothCommands.RECEIVE_DISCONNECT, -1);
	}
}

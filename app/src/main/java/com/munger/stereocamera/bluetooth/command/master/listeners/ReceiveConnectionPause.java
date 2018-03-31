package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ReceiveConnectionPause extends MasterIncoming
{
	public ReceiveConnectionPause()
	{
		super(BluetoothCommands.RECEIVE_CONNECTION_PAUSE, -1);
	}
}

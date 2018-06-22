package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class ReceiveZoom extends MasterIncoming
{
	public float zoom;

	public ReceiveZoom()
	{
		super(BluetoothCommands.RECEIVE_ZOOM, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		zoom = parent.readFloat();
	}
}

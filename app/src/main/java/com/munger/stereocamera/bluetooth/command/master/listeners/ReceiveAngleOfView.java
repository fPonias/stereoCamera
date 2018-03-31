package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.IOException;

public class ReceiveAngleOfView extends MasterIncoming
{
	public float x;
	public float y;

	public ReceiveAngleOfView()
	{
		super(BluetoothCommands.RECEIVE_ANGLE_OF_VIEW, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		x = parent.readFloat();
		y = parent.readFloat();
	}
}

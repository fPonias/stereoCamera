package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public abstract class MasterListener
{
	protected BluetoothCommands command;
	protected BluetoothMasterComm parent;

	public void setParent(BluetoothMasterComm parent)
	{
		this.parent = parent;
	}

	public MasterListener(BluetoothCommands command)
	{
		this.command = command;
	}

	public abstract void handleResponse() throws IOException;
	public abstract void onFail();

	public BluetoothCommands getCommand()
	{
		return command;
	}
}

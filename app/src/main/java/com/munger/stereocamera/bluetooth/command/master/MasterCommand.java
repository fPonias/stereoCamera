package com.munger.stereocamera.bluetooth.command.master;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public abstract class MasterCommand
{
	protected BluetoothMasterComm parent;

	public void setParent(BluetoothMasterComm parent)
	{
		this.parent = parent;
	}

	protected abstract void execute();
}

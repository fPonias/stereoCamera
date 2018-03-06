package com.munger.stereocamera.bluetooth.command.master.commands;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public abstract class MasterCommand
{
	protected BluetoothMasterComm parent;
	protected int id;

	public void setParent(BluetoothMasterComm parent)
	{
		this.parent = parent;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public abstract BluetoothCommands getCommand();
	public byte[] getArguments()
	{
		return null;
	}

	public void onStart()
	{}

	public abstract void onExecuteFail();
	public abstract void handleResponse() throws IOException;
}

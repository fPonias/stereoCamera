package com.munger.stereocamera.bluetooth.command.master.commands;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public abstract class MasterCommand
{
	MasterCommand() {}

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

	public void onExecuteFail() {}
	public MasterIncoming getResponse()
	{
		if (response == null)
			response = new MasterIncoming(getCommand(), id);

		return response;
	}

	public MasterIncoming response;
}
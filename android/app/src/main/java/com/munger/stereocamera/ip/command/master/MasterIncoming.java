package com.munger.stereocamera.ip.command.master;

import com.munger.stereocamera.ip.command.BluetoothCommands;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/29/18.
 */

public class MasterIncoming
{
	protected BluetoothCommands type;
	protected MasterComm parent;
	protected int id;

	public void setParent(MasterComm parent)
	{
		this.parent = parent;
	}

	public BluetoothCommands getType()
	{
		return type;
	}
	public void setType(BluetoothCommands type)
	{
		this.type = type;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}

	public MasterIncoming()
	{
		id = -1;
	}

	public MasterIncoming(BluetoothCommands type, int id)
	{
		this.type = type;
		this.id = id;
	}

	public void readResponse() throws IOException
	{}
}

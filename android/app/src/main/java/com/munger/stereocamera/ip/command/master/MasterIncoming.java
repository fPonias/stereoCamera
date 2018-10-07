package com.munger.stereocamera.ip.command.master;

import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/29/18.
 */

public class MasterIncoming
{
	protected Command.Type type;
	protected MasterComm parent;
	protected int id;

	public void setParent(MasterComm parent)
	{
		this.parent = parent;
	}

	public Command.Type getType()
	{
		return type;
	}
	public void setType(Command.Type type)
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

	public MasterIncoming(Command.Type type, int id)
	{
		this.type = type;
		this.id = id;
	}

	public void readResponse() throws IOException
	{}
}

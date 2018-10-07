package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public abstract class MasterCommand
{
	MasterCommand() {}

	protected MasterComm parent;
	protected int id;

	public void setParent(MasterComm parent)
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

	public abstract Command.Type getCommand();
	public byte[] getArguments()
	{
		return null;
	}

	public int getFileSz() { return 0; }
	public String getFilePath() { return null; }

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
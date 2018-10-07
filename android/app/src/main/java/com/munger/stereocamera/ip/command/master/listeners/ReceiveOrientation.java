package com.munger.stereocamera.ip.command.master.listeners;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/8/18.
 */

public class ReceiveOrientation extends MasterIncoming
{
	public PhotoOrientation orientation;

	public ReceiveOrientation()
	{
		super(Command.Type.RECEIVE_ORIENTATION, -1);
	}

	public void readResponse() throws IOException
	{
		byte ret = parent.readByte();
		orientation = PhotoOrientation.values()[ret];
	}
}

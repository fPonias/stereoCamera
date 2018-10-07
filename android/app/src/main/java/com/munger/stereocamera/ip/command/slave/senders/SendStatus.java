package com.munger.stereocamera.ip.command.slave.senders;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class SendStatus extends SlaveCommand
{
	private PreviewFragment.Status status;

	public SendStatus(PreviewFragment.Status status)
	{
		this.id = -1;
		this.command = Command.Type.RECEIVE_STATUS;
		this.status = status;
	}

	@Override
	public void send() throws IOException
	{
		comm.putInt(status.ordinal());
	}
}

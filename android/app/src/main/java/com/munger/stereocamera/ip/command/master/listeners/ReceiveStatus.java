package com.munger.stereocamera.ip.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterIncoming;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;

public class ReceiveStatus extends MasterIncoming
{
	public PreviewFragment.Status status;

	public ReceiveStatus()
	{
		super(Command.Type.RECEIVE_STATUS, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		int ret = parent.readInt();
		status = PreviewFragment.Status.values()[ret];

		Log.d("MasterComm", "command: " + Command.Type.RECEIVE_STATUS.name() + " success");
	}
}

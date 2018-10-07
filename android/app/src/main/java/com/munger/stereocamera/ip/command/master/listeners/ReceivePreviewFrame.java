package com.munger.stereocamera.ip.command.master.listeners;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;


public class ReceivePreviewFrame extends MasterIncoming
{
	public float zoom;
	public byte[] data;

	public ReceivePreviewFrame()
	{
		super(Command.Type.RECEIVE_PREVIEW_FRAME, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		zoom = parent.readFloat();
		int sz = parent.readInt();
		data = parent.readBytes(sz);
	}
}

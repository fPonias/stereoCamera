package com.munger.stereocamera.bluetooth.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;


public class ReceivePreviewFrame extends MasterIncoming
{
	public float zoom;
	public byte[] data;

	public ReceivePreviewFrame()
	{
		super(BluetoothCommands.RECEIVE_PREVIEW_FRAME, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		zoom = parent.readFloat();
		int sz = parent.readInt();
		data = parent.readBytes(sz);
	}
}

package com.munger.stereocamera.bluetooth.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;

public class ReceiveStatus extends MasterIncoming
{
	public PreviewFragment.Status status;

	public ReceiveStatus()
	{
		super(BluetoothCommands.RECEIVE_STATUS, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		int ret = parent.readInt();
		status = PreviewFragment.Status.values()[ret];

		Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.RECEIVE_STATUS.name() + " success");
	}
}

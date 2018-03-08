package com.munger.stereocamera.bluetooth.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;

public class ReceiveStatus extends MasterListener
{
	private Listener listener;

	public ReceiveStatus(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_STATUS);
		this.listener = listener;
	}

	@Override
	public void handleResponse() throws IOException
	{
		int ret = parent.readInt();
		PreviewFragment.Status status = PreviewFragment.Status.values()[ret];

		Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.RECEIVE_STATUS.name() + " success");
		listener.done(status);
	}

	@Override
	public void onFail()
	{
		listener.fail();
	}

	public interface Listener
	{
		void fail();
		void done(PreviewFragment.Status status);
	}
}

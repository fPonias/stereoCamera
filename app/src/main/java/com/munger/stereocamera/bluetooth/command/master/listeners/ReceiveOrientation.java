package com.munger.stereocamera.bluetooth.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/8/18.
 */

public class ReceiveOrientation extends MasterListener
{
	private Listener listener;

	public ReceiveOrientation(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_ORIENTATION);
		this.listener = listener;
	}

	@Override
	public void handleResponse() throws IOException
	{
		int ret = parent.ins.read();
		PhotoOrientation orientation = PhotoOrientation.values()[ret];
		listener.done(orientation);
	}

	@Override
	public void onFail()
	{
		listener.fail();
	}

	public interface Listener
	{
		void fail();
		void done(PhotoOrientation status);
	}
}

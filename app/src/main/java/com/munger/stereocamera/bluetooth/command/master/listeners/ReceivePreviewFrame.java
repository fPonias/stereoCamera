package com.munger.stereocamera.bluetooth.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;


public class ReceivePreviewFrame extends MasterListener
{
	private Listener listener;

	public ReceivePreviewFrame(Listener listener)
	{
		super(BluetoothCommands.RECEIVE_PREVIEW_FRAME);
		this.listener = listener;
	}

	@Override
	public void handleResponse() throws IOException
	{
		float zoom = parent.readFloat();
		int sz = parent.readInt();
		byte[] data = new byte[sz];
		int total = 0;
		int read = 1;
		while (read > 0 && total < sz)
		{
			read = parent.ins.read(data, total, sz - total);
			total += read;
		}

		listener.done(data, zoom);
	}

	@Override
	public void onFail()
	{
		listener.fail();
	}

	public interface Listener
	{
		void fail();
		void done(byte[] data, float zoom);
	}
}

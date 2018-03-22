package com.munger.stereocamera.bluetooth.utility;

import android.os.Handler;
import android.os.Looper;

import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendPreviewFrame;
import com.munger.stereocamera.widget.PreviewWidget;

/**
 * Created by hallmarklabs on 3/21/18.
 */

public class PreviewSender
{
	private final Object lock = new Object();
	private boolean cancelled = false;
	private PreviewWidget target;
	private BluetoothSlaveComm slaveComm;
	private Handler h;
	private static long FRAME_RATE = 250;

	public PreviewSender()
	{
		h = new Handler(Looper.getMainLooper());
	}

	public void setPreviewWidget(PreviewWidget target)
	{
		this.target = target;
	}

	public void setSlaveComm(BluetoothSlaveComm slaveComm)
	{
		this.slaveComm = slaveComm;
	}

	private Runnable getFrame = new Runnable() { public void run()
	{
		byte[] data = target.getPreviewFrame();

		if (data != null)
		{
			float zoom = target.getZoom();
			slaveComm.sendCommand(new SendPreviewFrame(data, zoom));
		}
	}};

	private Runnable frameLoop = new Runnable() { public void run()
	{
		while(!cancelled)
		{
			synchronized (lock)
			{
				try {lock.wait(FRAME_RATE);} catch(InterruptedException e){return;}

				if (cancelled)
					return;
			}

			h.post(getFrame);
		}
	}};

	private Thread t;

	public void start()
	{
		synchronized (lock)
		{
			if (t != null)
				return;

			t = new Thread(frameLoop);
		}

		t.start();
	}

	public void cancel()
	{
		synchronized (lock)
		{
			cancelled = true;
			lock.notify();
		}
	}
}

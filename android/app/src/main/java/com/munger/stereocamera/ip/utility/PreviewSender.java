package com.munger.stereocamera.ip.utility;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.munger.stereocamera.ip.command.slave.SlaveComm;
import com.munger.stereocamera.ip.command.slave.senders.SendPreviewFrame;
import com.munger.stereocamera.widget.PreviewWidget;

/**
 * Created by hallmarklabs on 3/21/18.
 */

public class PreviewSender
{
	private final Object lock = new Object();
	private boolean cancelled = false;
	private PreviewWidget target;
	private SlaveComm slaveComm;
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

	public void setSlaveComm(SlaveComm slaveComm)
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
					break;
			}

			h.post(getFrame);
		}

		synchronized (lock)
		{
			senderThread = null;
		}
	}};

	private String getLogTag(){return "preview sender";}

	private Thread senderThread;

	public void start()
	{
		Log.d(getLogTag(), "starting preview sender");
		synchronized (lock)
		{
			if (senderThread != null)
				return;

			cancelled = false;
			senderThread = new Thread(frameLoop);
		}

		senderThread.start();
		Log.d(getLogTag(), "started preview sender");
	}

	public void cancel()
	{
		Log.d(getLogTag(), "cancelling preview sender");
		synchronized (lock)
		{
			cancelled = true;
			lock.notify();
		}
	}
}

package com.munger.stereocamera.bluetooth.utility;

import android.util.Log;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.GetRemoteLatency;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class GetLatency
{
	private Listener listener;
	private BluetoothMasterComm masterComm;

	public GetLatency(Listener listener, long timeout)
	{
		this.listener = listener;
		this.timeout = timeout;

		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
	}

	private long localLatency;
	private long remoteLatency;
	private final Object latencyLock = new Object();
	private long timeout;

	public interface Listener
	{
		void trigger(ListenerListener listener);
		void pong(long localLatency, long remoteLatency);
		void fail();
	}

	public interface ListenerListener
	{
		void reply();
	}

	public void execute()
	{
		this.listener = listener;
		this.timeout = timeout;

		localLatency = -1;
		remoteLatency = -1;
		Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " called");

		getLocalLatency();
		getRemoteLatency();
		startTimeout();
	}

	private void getLocalLatency()
	{
		final long now = System.currentTimeMillis();

		listener.trigger(new ListenerListener() { public void reply()
		{
			synchronized (latencyLock)
			{
				localLatency = System.currentTimeMillis() - now;
			}

			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " local check success");
			done();
		}});
	}

	private void getRemoteLatency()
	{
		masterComm.runCommand(new GetRemoteLatency(new GetRemoteLatency.Listener()
		{
			@Override
			public void done(long latency)
			{
				synchronized (latencyLock)
				{
					remoteLatency = latency;
				}

				GetLatency.this.done();
			}

			@Override
			public void fail()
			{
				GetLatency.this.fail();
			}
		}));
	}

	private void startTimeout()
	{
		Thread foo = new Thread(new Runnable() {public void run()
		{
			synchronized (latencyLock)
			{
				try{latencyLock.wait(timeout);}catch(InterruptedException e){}
			}

			done();
		}});
		foo.start();
	}

	private boolean finished = false;

	private void done()
	{
		synchronized (latencyLock)
		{
			if (finished)
				return;

			finished = true;
		}

		if (localLatency == -1 || remoteLatency == -1)
		{
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " failed");
			listener.fail();
		}
		else
		{
			Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " success");
			listener.pong(localLatency, remoteLatency);
		}
	}

	private void fail()
	{
		synchronized (latencyLock)
		{
			if (finished)
				return;

			finished = true;
		}

		listener.fail();
	}
}

package com.munger.stereocamera.ip.utility;

import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.master.MasterIncoming;
import com.munger.stereocamera.ip.command.master.commands.GetRemoteLatency;
import com.munger.stereocamera.fragment.PreviewFragment;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class GetLatency
{
	private Listener listener;
	private MasterComm masterComm;
	private PreviewFragment target;

	public GetLatency(Listener listener, PreviewFragment fragment, long timeout)
	{
		this.listener = listener;
		this.target = fragment;
		this.timeout = timeout;

		masterComm = MainActivity.getInstance().getCtrl().getMasterComm();
	}

	private long localLatency;
	private boolean localDone = false;
	private long remoteLatency;
	private boolean remoteDone = false;
	private final Object latencyLock = new Object();
	private long timeout;

	public interface Listener
	{
		void pong(long localLatency, long remoteLatency);
		void fail();
	}

	public interface LocalLatencyTrigger
	{
		void trigger(long delay, LocalLatencyTriggerListener listener);
	}

	public interface LocalLatencyTriggerListener
	{
		void reply();
	}

	public void execute(long localDelay)
	{
		localLatency = -1;
		remoteLatency = -1;
		Log.d("MasterComm", "command: " + Command.Type.LATENCY_CHECK.name() + " called");

		getLocalLatency(localDelay);
		getRemoteLatency();
		startTimeout();
	}

	private long localNow;

	private PreviewFragment.LatencyListener localListener = new PreviewFragment.LatencyListener()
	{
		@Override
		public void triggered()
		{
			synchronized (latencyLock)
			{
				localLatency = System.currentTimeMillis() - localNow;
			}

			Log.d("MasterComm", "command: " + Command.Type.LATENCY_CHECK.name() + " local check success");
		}

		@Override
		public void done()
		{
			Log.d("MasterComm", "command: " + Command.Type.LATENCY_CHECK.name() + " local check finished");

			synchronized (latencyLock)
			{
				localDone = true;
			}

			GetLatency.this.done();
		}

		@Override
		public void fail()
		{
			synchronized (latencyLock)
			{
				localDone = true;
			}

			GetLatency.this.done();
		}
	};

	private void getLocalLatency(final long delay)
	{
		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try {Thread.sleep(delay); } catch(InterruptedException e){}
				localNow = System.currentTimeMillis();
				target.getLatency(delay, localListener);
			}
		});
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	private void getRemoteLatency()
	{
		masterComm.runCommand(new GetRemoteLatency(), new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				GetRemoteLatency.Response r = (GetRemoteLatency.Response) response;
				synchronized (latencyLock)
				{
					remoteLatency = r.latency;
					remoteDone = true;
				}

				GetLatency.this.done();
			}

			@Override
			public void onFail()
			{
				synchronized (latencyLock)
				{
					remoteDone = true;
				}

				GetLatency.this.fail();
			}
		});
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
			if (!localDone || !remoteDone)
				return;

			if (finished)
				return;

			finished = true;
		}

		if (localLatency == -1 || remoteLatency == -1)
		{
			Log.d("MasterComm", "command: " + Command.Type.LATENCY_CHECK.name() + " failed");
			listener.fail();
		}
		else
		{
			Log.d("MasterComm", "command: " + Command.Type.LATENCY_CHECK.name() + " success");
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

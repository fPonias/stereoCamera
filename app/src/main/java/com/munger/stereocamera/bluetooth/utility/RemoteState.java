package com.munger.stereocamera.bluetooth.utility;

import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveAngleOfView;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveStatus;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveZoom;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.util.ArrayList;

public class RemoteState
{
	public int status;
	public float zoom;
	public float horizFov;
	public float vertFov;
	public ReceiveGravity.Gravity gravity;

	public static class Listener
	{
		public void onStatus(int status) {}
		public void onZoom(float zoom) {}
		public void onFov(float horiz, float vert) {}
		public void onGravity(ReceiveGravity.Gravity gravity) {}
	}

	public RemoteState(BluetoothMasterComm comm)
	{
		this.comm = comm;
	}

	private BluetoothMasterComm comm;
	private ArrayList<Listener> listeners = new ArrayList<>();

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		if (listeners.contains(listener))
			listeners.remove(listener);
	}

	public void start()
	{
		comm.registerListener(new ReceiveStatus(new ReceiveStatus.Listener()
		{
			@Override
			public void fail()
			{}

			@Override
			public void done(int status)
			{
				RemoteState.this.status = status;

				for(Listener listener : listeners)
					listener.onStatus(status);
			}
		}));

		comm.registerListener(new ReceiveAngleOfView(new ReceiveAngleOfView.Listener()
		{
			@Override
			public void done(float horiz, float vert)
			{
				RemoteState.this.horizFov = horiz;
				RemoteState.this.vertFov = vert;

				for(Listener listener : listeners)
					listener.onFov(horiz, vert);
			}

			@Override
			public void fail()
			{}
		}));

		comm.registerListener(new ReceiveGravity(new ReceiveGravity.Listener()
		{
			@Override
			public void done(ReceiveGravity.Gravity value)
			{
				gravity = value;
				for(Listener listener : listeners)
					listener.onGravity(gravity);
			}

			@Override
			public void fail()
			{}
		}));

		comm.registerListener(new ReceiveZoom(new ReceiveZoom.Listener()
		{
			@Override
			public void done(float zoom)
			{
				RemoteState.this.zoom = zoom;
				for(Listener listener : listeners)
					listener.onZoom(zoom);
			}

			@Override
			public void fail()
			{}
		}));
	}

	private final Object lock = new Object();
	private Listener readyListener = new Listener()
	{
		@Override
		public void onStatus(int status)
		{
			synchronized (lock)
			{
				if (RemoteState.this.status == PreviewFragment.READY)
					lock.notify();
			}
		}
	};

	public boolean waitOnReady(long timeout)
	{
		if (status == PreviewFragment.READY)
			return true;

		Thread t = new Thread(new Runnable() { public void run()
		{
			listeners.add(readyListener);
		}});
		t.start();

		synchronized (lock)
		{
			if (status != PreviewFragment.READY)
				try {lock.wait(timeout);} catch(InterruptedException e){}
		}

		listeners.remove(readyListener);

		return (status == PreviewFragment.READY);
	}

	public interface ReadyListener
	{
		void done();
		void fail();
	}

	public void waitOnReadyAsync(final long timeout, final ReadyListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			boolean success = waitOnReady(timeout);

			if (success)
				listener.done();
			else
				listener.fail();
		}});
	}
}

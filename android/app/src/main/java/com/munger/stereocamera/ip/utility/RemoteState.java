package com.munger.stereocamera.ip.utility;

import android.util.Log;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.ip.command.commands.SendGravity;
import com.munger.stereocamera.ip.command.commands.SendStatus;
import com.munger.stereocamera.ip.command.commands.SendZoom;

import java.util.ArrayList;

public class RemoteState
{
	public PreviewFragment.Status status;
	public float zoom;
	public float horizFov;
	public float vertFov;
	public SendGravity.Gravity gravity;
	public PhotoOrientation orientation;

	public static class Listener
	{
		public void onStatus(PreviewFragment.Status status) {}
		public void onZoom(float zoom) {}
		public void onGravity(SendGravity.Gravity gravity) {}
		public void onConnectionPause() {}
		public void onDisconnect() {}
	}

	public RemoteState(CommCtrl comm)
	{
		this.comm = comm;
		orientation = PhotoOrientation.DEG_0;
	}

	private CommCtrl comm;
	private ArrayList<Listener> listeners = new ArrayList<>();
	private final Object listenerLock = new Object();

	public void addListener(Listener listener)
	{
		synchronized (listenerLock)
		{
			if (!listeners.contains(listener))
				listeners.add(listener);
		}
	}

	public void removeListener(Listener listener)
	{
		synchronized (listenerLock)
		{
			if (listeners.contains(listener))
				listeners.remove(listener);
		}
	}

	public void start()
	{
		comm.registerListener(Command.Type.RECEIVE_STATUS, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SendStatus r = (SendStatus) command;
			RemoteState.this.status = r.status;

			synchronized (lock)
			{
				lock.notify();
			}

			synchronized (listenerLock)
			{
				for (Listener listener : listeners)
					listener.onStatus(r.status);
			}
		}});

		listeners.add(readyListener);

		comm.registerListener(Command.Type.RECEIVE_GRAVITY, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SendGravity r = (SendGravity) command;
			gravity = r.gravity;

			synchronized (listenerLock)
			{
				for (Listener listener : listeners)
					listener.onGravity(gravity);
			}
		}});

		comm.registerListener(Command.Type.RECEIVE_ZOOM, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SendZoom r = (SendZoom) command;
			RemoteState.this.zoom = r.zoom;

			synchronized (listenerLock)
			{
				for (Listener listener : listeners)
					listener.onZoom(r.zoom);
			}
		}});

		comm.registerListener(Command.Type.CONNECTION_PAUSE, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			synchronized (listenerLock)
			{
				for (Listener listener : listeners)
					listener.onConnectionPause();
			}
		}});

		comm.registerListener(Command.Type.DISCONNECT, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			synchronized (listenerLock)
			{
				for (Listener listener : listeners)
					listener.onDisconnect();
			}
		}});
	}

	private final Object lock = new Object();
	private Listener readyListener = new Listener()
	{
		@Override
		public void onStatus(PreviewFragment.Status status)
		{
			Log.d("stereoCamera", "status " + status.name() + " received");
			synchronized (lock)
			{
				if (RemoteState.this.status == PreviewFragment.Status.READY)
					lock.notify();
			}
		}
	};

	public boolean waitOnStatus(PreviewFragment.Status status, long timeout)
	{
		return waitOnStatus(new PreviewFragment.Status[] {status}, timeout);
	}

	public boolean waitOnStatus(PreviewFragment.Status[] statuss, long timeout)
	{
		if (isPresent(this.status, statuss))
			return true;

		long then = System.currentTimeMillis();
		long diff = 0;

		do
		{
			synchronized (lock)
			{
				if (!isPresent(status, statuss))
					try {lock.wait(timeout - diff);} catch(InterruptedException e){}
			}

			long now = System.currentTimeMillis();
			diff = now - then;
		} while (diff < timeout && !isPresent(this.status, statuss));

		return (isPresent(status, statuss));
	}

	private boolean isPresent(PreviewFragment.Status status, PreviewFragment.Status[] statuss)
	{
		for (PreviewFragment.Status st : statuss)
		{
			if (status == st)
				return true;
		}

		return false;
	}

	public interface ReadyListener
	{
		void done();
		void fail();
	}

	public void waitOnStatusAsync(final PreviewFragment.Status status, final long timeout, final ReadyListener listener)
	{
		waitOnStatusAsync(new PreviewFragment.Status[] { status }, timeout, listener);
	}

	public void waitOnStatusAsync(final PreviewFragment.Status[] statuss, final long timeout, final ReadyListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			boolean success = waitOnStatus(statuss, timeout);

			if (success)
				listener.done();
			else
				listener.fail();
		}});
		t.start();
	}
}

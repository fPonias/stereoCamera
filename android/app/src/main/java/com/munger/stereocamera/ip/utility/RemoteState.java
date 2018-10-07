package com.munger.stereocamera.ip.utility;

import android.util.Log;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.master.MasterIncoming;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveAngleOfView;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveOrientation;
import com.munger.stereocamera.ip.command.master.listeners.ReceivePreviewFrame;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveStatus;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveZoom;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.util.ArrayList;

public class RemoteState
{
	public PreviewFragment.Status status;
	public float zoom;
	public float horizFov;
	public float vertFov;
	public ReceiveGravity.Gravity gravity;
	public PhotoOrientation orientation;

	public static class Listener
	{
		public void onStatus(PreviewFragment.Status status) {}
		public void onZoom(float zoom) {}
		public void onFov(float horiz, float vert) {}
		public void onGravity(ReceiveGravity.Gravity gravity) {}
		public void onOrientation(PhotoOrientation orientation) {}
		public void onConnectionPause() {}
		public void onDisconnect() {}
		public void onPreviewFrame(byte[] data, float zoom) {}
	}

	public RemoteState(MasterComm comm)
	{
		this.comm = comm;
		orientation = PhotoOrientation.DEG_0;
	}

	private MasterComm comm;
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
		comm.registerListener(Command.Type.RECEIVE_STATUS, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				ReceiveStatus r = (ReceiveStatus) response;
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
			}
		});

		listeners.add(readyListener);

		comm.registerListener(Command.Type.RECEIVE_ANGLE_OF_VIEW, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				ReceiveAngleOfView r = (ReceiveAngleOfView) response;
				RemoteState.this.horizFov = r.x;
				RemoteState.this.vertFov = r.y;

				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onFov(r.x, r.y);
				}
			}
		});

		comm.registerListener(Command.Type.RECEIVE_GRAVITY, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				ReceiveGravity r = (ReceiveGravity) response;
				gravity = r.gravity;

				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onGravity(gravity);
				}
			}
		});

		comm.registerListener(Command.Type.RECEIVE_ZOOM, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				ReceiveZoom r = (ReceiveZoom) response;
				RemoteState.this.zoom = r.zoom;

				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onZoom(r.zoom);
				}
			}
		});

		comm.registerListener(Command.Type.RECEIVE_ORIENTATION, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				ReceiveOrientation r = (ReceiveOrientation) response;
				RemoteState.this.orientation = r.orientation;

				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onOrientation(r.orientation);
				}
			}
		});

		comm.registerListener(Command.Type.RECEIVE_CONNECTION_PAUSE, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onConnectionPause();
				}
			}
		});

		comm.registerListener(Command.Type.RECEIVE_DISCONNECT, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onDisconnect();
				}
			}
		});

		comm.registerListener(Command.Type.RECEIVE_PREVIEW_FRAME, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				ReceivePreviewFrame r = (ReceivePreviewFrame) response;

				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onPreviewFrame(r.data, r.zoom);
				}
			}
		});
	}

	private final Object lock = new Object();
	private Listener readyListener = new Listener()
	{
		@Override
		public void onStatus(PreviewFragment.Status status)
		{
			Log.d("Remote State", "status " + status.name() + " received");
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

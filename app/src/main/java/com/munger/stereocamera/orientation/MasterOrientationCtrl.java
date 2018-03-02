package com.munger.stereocamera.orientation;

import android.os.Handler;
import android.os.Looper;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.GetGravity;

public class MasterOrientationCtrl
{
	private OrientationWidget indicator;
	private final Object glock = new Object();
	private Thread gthread;
	private boolean cancelPoll = false;
	private BluetoothMasterComm masterComm;
	private Handler handler;

	public MasterOrientationCtrl(OrientationWidget target)
	{
		this.indicator = target;
		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
		handler = new Handler(Looper.getMainLooper());
	}

	private GetGravity.Listener listener = new GetGravity.Listener()
	{
		@Override
		public void done(GetGravity.Gravity value)
		{
			float zval = value.x * value.x + value.y * value.y;
			double azrad = Math.atan2(Math.sqrt(zval), value.z);
			final double az = azrad * 180.0 / Math.PI;

			handler.post(new Runnable() {public void run()
			{
				indicator.setRotation2((float) az - 90.0f);
			}});

			synchronized (glock)
			{
				glock.notify();
			}
		}

		@Override
		public void fail()
		{
			synchronized (glock)
			{
				glock.notify();
			}
		}
	};

	public void start()
	{
		synchronized (glock)
		{
			if (gthread != null)
				return;

			gthread = new Thread(new Runnable() { public void run()
			{
				poll();
			}});
		}

		gthread.start();
	}

	private void poll()
	{
		cancelPoll = false;
		while(cancelPoll == false)
		{
			try{Thread.sleep(150);} catch(InterruptedException e){break;}

			masterComm.runCommand(new GetGravity(listener));
			synchronized (glock)
			{
				try {glock.wait();} catch(InterruptedException e) {break;}
			}
		}

		synchronized (glock)
		{
			cancelPoll = false;
			gthread = null;
		}
	}

	public void onPause()
	{
		cancelPoll = true;
		synchronized (glock)
		{
			glock.notify();
		}
	}
}

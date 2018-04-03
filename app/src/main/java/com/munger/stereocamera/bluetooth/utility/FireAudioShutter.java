package com.munger.stereocamera.bluetooth.utility;

import android.content.Context;
import android.util.Log;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.bluetooth.command.master.commands.AudioSyncStart;
import com.munger.stereocamera.bluetooth.command.master.commands.AudioSyncTriggered;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.utility.AudioSync;
import com.munger.stereocamera.utility.PhotoFiles;

public class FireAudioShutter extends FireShutterAbstract
{
	private AudioSync audioSync;
	private boolean started = false;
	private boolean audioTriggered = false;
	private boolean remoteTriggered = false;
	private boolean localTriggered = false;
	private final Object lock = new Object();

	public FireAudioShutter(MasterFragment masterFragment)
	{
		super(masterFragment);

		audioSync = new AudioSync();
		masterComm.registerListener(BluetoothCommands.RECEIVE_AUDIO_SYNC_TRIGGERED, audioSyncListener);
	}

	private String getTag() { return "FireAudioShutter"; }

	private BluetoothMasterComm.SlaveListener audioSyncListener = new BluetoothMasterComm.SlaveListener()
	{
		@Override
		public void onResponse(MasterIncoming response)
		{
			Log.d(getTag(), "remote shutter triggered");

			boolean doLocal = true;

			synchronized (lock)
			{
				if (audioTriggered)
					return;

				audioTriggered = true;
				remoteTriggered = true;

				if (localTriggered == false)
				{
					doLocal = true;
					localTriggered = true;
				}
			}

			if (doLocal)
				fireLocal(0);

			audioSync.stop();
		}
	};

	public void execute(short threshold, final Listener listener)
	{
		synchronized (lock)
		{
			if (started)
				listener.fail();

			started = true;
			audioTriggered = false;
			remoteTriggered = false;
			this.listener = listener;
		}

		this.listener = listener;

		Log.d(getTag(), "sending audio sync command");
		masterComm.runCommand(new AudioSyncStart(), new BluetoothMasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				Log.d(getTag(), "remote data received");
				synchronized (lock)
				{
					AudioSyncStart.Response r = (AudioSyncStart.Response) response;
					handleRemote(r.data, r.orientation, r.zoom);
				}
			}
		});

		audioTriggered = false;

		Log.d(getTag(), "starting audio sync with threshold " + threshold);
		audioSync.start(new AudioSync.Listener() { public void trigger()
		{
			Log.d(getTag(), "audio sync triggered");
			boolean doLocal = false;
			synchronized (lock)
			{
				if (audioTriggered)
					return;

				audioTriggered = true;

				if (!remoteTriggered)
				{
					Log.d(getTag(), "sending trigger notification");
					masterComm.runCommand(new AudioSyncTriggered(), null);
				}

				if (localTriggered == false)
				{
					doLocal = true;
					localTriggered = true;
				}
			}

			if (doLocal)
				fireLocal(0);

			audioSync.stop();
		}}, threshold);
	}

	protected void onDone()
	{
		synchronized (lock)
		{
			audioTriggered = false;
			started = false;
			audioSync.stop();
		}

		super.onDone();
	}

	protected void onFail()
	{
		synchronized (lock)
		{
			audioTriggered = false;
			started = false;
			audioSync.stop();
		}

		super.onFail();
	}
}

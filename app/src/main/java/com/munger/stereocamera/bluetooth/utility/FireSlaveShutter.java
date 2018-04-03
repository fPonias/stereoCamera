package com.munger.stereocamera.bluetooth.utility;

import android.util.Log;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendAudioSyncTriggered;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.fragment.SlaveFragment;
import com.munger.stereocamera.utility.AudioSync;

public class FireSlaveShutter
{
	private AudioSync audioSync;
	private boolean started = false;
	private boolean triggered = false;
	private final Object audioLock = new Object();
	private byte[] imgBytes;
	private BluetoothSlaveComm slaveComm;

	private SlaveFragment fragment;

	public FireSlaveShutter(SlaveFragment fragment)
	{
		this.fragment = fragment;
		audioSync = new AudioSync();
		slaveComm = MyApplication.getInstance().getBtCtrl().getSlave().getComm();
		slaveComm.addListener(BluetoothCommands.AUDIO_SYNC_TRIGGERED, slaveListener);
	}


	private String getTag() {return "Fire Slave Shutter";}

	private BluetoothSlaveComm.Listener slaveListener = new BluetoothSlaveComm.Listener()
	{
		@Override
		public void onCommand(SlaveCommand command)
		{
			Log.d(getTag(), "remote shutter triggered");
			executeByAudio2();
		}
	};

	public interface AudioListener
	{
		void done(byte[] data);
	}

	private byte[] executeRet;
	public byte[] executeByAudioSync(int id, short threshold)
	{
		synchronized (audioLock)
		{
			if (started)
				return null;

			executeRet = null;
		}

		executeByAudio(id, threshold, new AudioListener() { public void done(byte[] data)
		{
			synchronized (audioLock)
			{
				executeRet = data;
				audioLock.notify();
			}
		}});

		synchronized (audioLock)
		{
			if (executeRet == null)
				try{audioLock.wait(2000);} catch(InterruptedException e){}
		}

		return executeRet;
	}

	private AudioListener listener;

	public void executeByAudio(final int id, short threshold, AudioListener listener)
	{
		synchronized (audioLock)
		{
			if (started)
				return;

			started = true;

			triggered = false;
			imgBytes = null;
			this.listener = listener;
		}

		Log.d(getTag(), "starting audio sync with threshold " + threshold);

		//audioSync.setDebugging(true);
		audioSync.start(new AudioSync.Listener()
		{
			@Override
			public void trigger()
			{
				Log.d(getTag(), "audio sync triggered");
				slaveComm.sendCommand(new SendAudioSyncTriggered(id));
				executeByAudio2();
			}
		}, threshold);
	}

	private void executeByAudio2()
	{
		synchronized (audioLock)
		{
			if (triggered)
			{
				Log.d(getTag(), "shutter already fired");
				return;
			}

			triggered = true;
		}

		audioSync.stop();

		imgBytes = doFireShutter();

		listener.done(imgBytes);

		synchronized (audioLock)
		{
			started = false;
		}
	}

	public byte[] doFireShutter()
	{
		Log.d(getTag(), "firing shutter");
		final Object lock = new Object();
		imgBytes = null;

		fragment.fireShutter(new PreviewFragment.ImageListener()
		{
			@Override
			public void onImage(byte[] bytes)
			{
				synchronized (lock)
				{
					imgBytes = bytes;
					lock.notify();
				}
			}

			@Override
			public void onFinished()
			{
				synchronized (lock)
				{
					started = false;
				}
			}
		});

		synchronized (lock)
		{
			if (imgBytes == null)
				try{lock.wait(2000);}catch(InterruptedException e){}
		}

		return imgBytes;
	}
}

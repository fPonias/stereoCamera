package com.munger.stereocamera;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.BluetoothSlave;
import com.munger.stereocamera.utility.InteractiveReceiver;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.SendProcessedPhoto;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.service.PhotoProcessorServiceReceiver;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.widget.AudioSource;
import com.munger.stereocamera.widget.OrientationCtrl;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hallmarklabs on 2/28/18.
 */

public class MyApplication extends Application
{
	private static MyApplication instance;

	public static MyApplication getInstance()
	{
		return instance;
	}

	public static final String BT_SERVICE_NAME = "stereoCamera";

	private BluetoothCtrl btCtrl;
	private OrientationCtrl orientationCtrl;
	private Preferences prefs;
	private PhotoProcessorServiceReceiver photoReceiver;
	private InteractiveReceiver interactiveReceiver;
	private AudioSource audioSource;

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;

		IntentFilter filter = new IntentFilter(PhotoProcessorService.BROADCAST_PROCESSED_ACTION);
		photoReceiver = new PhotoProcessorServiceReceiver(new PhotoProcessorServiceReceiver.Listener()
		{
			@Override
			public void onPhoto(String path)
			{
				handleProcessedPhoto(path);
			}
		});
		LocalBroadcastManager.getInstance(this).registerReceiver(photoReceiver, filter);


		interactiveReceiver = new InteractiveReceiver();
		interactiveReceiver.addListener(new InteractiveReceiver.Listener()
		{
			@Override
			public void screenChanged(boolean isOn)
			{
				for (Listener listener : listeners)
					listener.onScreenChanged(isOn);
			}
		});

		filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(interactiveReceiver, filter);

		filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		registerReceiver(interactiveReceiver, filter);

		audioSource = new AudioSource(this);
		audioSource.start();
	}

	@Override
	public void onTerminate()
	{
		unregisterReceiver(interactiveReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(photoReceiver);

		audioSource.stop();

		super.onTerminate();
	}

	public AudioSource getAudioSource()
	{
		return audioSource;
	}

	private PhotoFiles photoFiles = null;

	private void handleProcessedPhoto(final String path)
	{
		if (photoFiles == null)
			photoFiles = new PhotoFiles(this);

		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				handlePhotoProcessed2(path);
			}

			@Override
			public void fail()
			{

			}
		});
	}

	private void handlePhotoProcessed2(String path)
	{
		File fl = new File(path);
		String newPath = photoFiles.saveNewFile(fl);

		Toast.makeText(MyApplication.this, R.string.new_photo_available, Toast.LENGTH_LONG).show();

		if (btCtrl != null && btCtrl.getMaster() != null && btCtrl.getMaster().getComm() != null)
		{
			BluetoothMasterComm comm = btCtrl.getMaster().getComm();
			comm.runCommand(new SendProcessedPhoto(newPath), null);
		}

		for (Listener listener : listeners)
			listener.onNewPhoto(newPath);
	}


	public void setupBTServer(BluetoothCtrl.SetupListener listener)
	{
		if (btCtrl == null)
			btCtrl = new BluetoothCtrl(this);

		if (!btCtrl.getIsSetup())
		{
			btCtrl.setup(listener);
			return;
		}

		listener.onSetup();
	}

	public BluetoothCtrl getBtCtrl()
	{
		return btCtrl;
	}

	public Preferences getPrefs()
	{
		return prefs;
	}

	public OrientationCtrl getOrientationCtrl()
	{
		return orientationCtrl;
	}

	private BaseActivity currentActivity;

	public void setCurrentActivity(BaseActivity activity)
	{
		currentActivity = activity;

		if (prefs == null)
		{
			prefs = new Preferences();
			prefs.setup();
		}
	}

	public BaseActivity getCurrentActivity()
	{
		return currentActivity;
	}

	public static class Listener
	{
		public void onNewPhoto(String path) {}
		public void onScreenChanged(boolean isOn) {}
		public void onBluetoothChanged(boolean isConnected, BluetoothDevice device) {}
	}

	private ArrayList<Listener> listeners = new ArrayList<>();

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	public void handleDisconnection()
	{
		for (Listener listener : listeners)
			listener.onBluetoothChanged(false, null);
	}

	public boolean isMasterConnected()
	{
		if (btCtrl == null)
			return false;

		BluetoothMaster master = btCtrl.getMaster();

		if (master == null)
			return false;

		return master.isConnected();
	}

	public boolean isSlaveConnected()
	{
		if (btCtrl == null)
			return false;

		BluetoothSlave slave = btCtrl.getSlave();

		if (slave == null)
			return false;

		return slave.isConnected();
	}

	public void cleanUpConnections()
	{
		if (btCtrl == null)
			return;

		BluetoothMaster btmaster = btCtrl.getMaster();
		if (btmaster != null && isMasterConnected())
			btmaster.cleanUp();

		BluetoothSlave btslave = btCtrl.getSlave();
		if (btslave != null && isSlaveConnected())
			btslave.cleanUp();
	}
}

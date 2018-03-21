package com.munger.stereocamera;

import android.app.Application;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.Preferences;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.SendProcessedPhoto;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.service.PhotoProcessorServiceReceiver;
import com.munger.stereocamera.utility.PhotoFiles;
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
	private PhotoProcessorServiceReceiver receiver;

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;

		IntentFilter filter = new IntentFilter(PhotoProcessorService.BROADCAST_PROCESSED_ACTION);
		receiver = new PhotoProcessorServiceReceiver(new PhotoProcessorServiceReceiver.Listener()
		{
			@Override
			public void onPhoto(String path)
			{
				handleProcessedPhoto(path);
			}
		});
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
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
			comm.runCommand(new SendProcessedPhoto(newPath));
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

	public interface Listener
	{
		void onNewPhoto(String path);
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
}

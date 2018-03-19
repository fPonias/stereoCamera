package com.munger.stereocamera;

import android.app.Application;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.Preferences;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.service.PhotoProcessorServiceReceiver;
import com.munger.stereocamera.widget.OrientationCtrl;

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
				for (Listener listener : listeners)
					listener.onNewPhoto(path);
			}
		});
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
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

package com.munger.stereocamera;

import android.app.Application;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.Preferences;
import com.munger.stereocamera.widget.OrientationCtrl;

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

	@Override
	public void onCreate()
	{
		super.onCreate();
		instance = this;
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
}

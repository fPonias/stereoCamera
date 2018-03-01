package com.munger.stereocamera;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;

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

	public static int DISCOVER_TIMEOUT = 30000;
	public static int LISTEN_TIMEOUT = 30000;

	private BluetoothCtrl btCtrl;

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

	private BaseActivity currentActivity;

	public void setCurrentActivity(BaseActivity activity)
	{
		currentActivity = activity;
	}

	public BaseActivity getCurrentActivity()
	{
		return currentActivity;
	}
}

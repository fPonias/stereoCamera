package com.munger.stereocamera;

import android.app.Application;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.utility.Preferences;

public class MyApplication extends Application
{
	private static MyApplication instance;

	public static MyApplication getInstance()
	{
		return instance;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		instance = this;
	}

	private BluetoothCtrl btCtrl;

	public BluetoothCtrl getBtCtrl()
	{
		return btCtrl;
	}

	public void setBtCtrl(BluetoothCtrl btCtrl)
	{
		this.btCtrl = btCtrl;
	}

	private Preferences prefs;

	public Preferences getPrefs()
	{
		return prefs;
	}

	public void setPrefs(Preferences prefs)
	{
		this.prefs = prefs;
	}
}

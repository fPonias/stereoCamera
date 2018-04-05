package com.munger.stereocamera;

import android.app.Application;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;

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
}

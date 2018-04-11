package com.munger.stereocamera;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.google.android.gms.ads.MobileAds;
import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.utility.Preferences;

import java.util.jar.Attributes;

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

		try
		{
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			String name = pi.versionName;

			if (name.endsWith("-paid"))
				adsEnabled = false;
			else
			{
				adsEnabled = true;
				MobileAds.initialize(this, "ca-app-pub-9089181112526283~1419408286");
			}
		}
		catch(PackageManager.NameNotFoundException e)
		{}
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

	private boolean adsEnabled;

	public boolean getAdsEnabled()
	{
		return adsEnabled;
	}
}

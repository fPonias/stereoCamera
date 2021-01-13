package com.munger.stereocamera;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import androidx.room.Room;

import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.ethernet.EthernetCtrl;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.utility.data.AppDatabase;
import com.munger.stereocamera.utility.data.ClientViewModel;
import com.munger.stereocamera.utility.data.ClientViewModelProvider;

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

		db = Room.databaseBuilder(this, AppDatabase.class, "client-db")
				.fallbackToDestructiveMigration()
				.build();

		try
		{
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			String name = pi.versionName;

			if (name.endsWith("-paid"))
				adsEnabled = false;
			else
			{
				adsEnabled = true;
				//MobileAds.initialize(this, "ca-app-pub-9089181112526283~1419408286");
			}
		}
		catch(PackageManager.NameNotFoundException e)
		{}
	}

	private CommCtrl ctrl;
	private BluetoothCtrl btCtrl;
	private EthernetCtrl ethCtrl;
	private AppDatabase db;

	public static final String BT_SERVICE_NAME = "stereoCamera";

	public void setupBTServer(IPListeners.SetupListener listener)
	{
		if (btCtrl == null)
		{
			btCtrl = new BluetoothCtrl(MainActivity.getInstance());
		}

		if (!btCtrl.getIsSetup())
		{
			btCtrl.setup(listener);
			return;
		}

		listener.onSetup();
	}

	public CommCtrl getCtrl()
	{
		return ctrl;
	}

	public void setCtrl(CommCtrl value)
	{
		ctrl = value;
	}

	public BluetoothCtrl getBtCtrl()
	{
		return btCtrl;
	}

	public EthernetCtrl getEthCtrl()
	{
		return ethCtrl;
	}

	public AppDatabase getDatabase()
	{
		return db;
	}

	public void setupEthernetServer(IPListeners.SetupListener listener)
	{
		if (ethCtrl == null)
		{
			ethCtrl = new EthernetCtrl();
		}

		if (!ethCtrl.getIsSetup())
		{
			ethCtrl.setup(listener);
			return;
		}

		listener.onSetup();
	}

	private boolean adsEnabled;

	public boolean getAdsEnabled()
	{
		return adsEnabled;
	}


}

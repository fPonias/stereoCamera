package com.munger.stereocamera.bluetooth;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.munger.stereocamera.MainActivity;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Set;
import java.util.UUID;

public class BluetoothCtrl
{
	public BluetoothCtrl(MainActivity parent)
	{
		this.parent = parent;
		adapter = BluetoothAdapter.getDefaultAdapter();
	}

	private BluetoothAdapter adapter;
	private MainActivity parent;
	private boolean isSetup = false;
	private final Object lock = new Object();

	private BluetoothDiscoverer discoverer;
	private BluetoothSlave slave;
	private BluetoothMaster master;

	public static final UUID APP_ID = UUID.fromString("e1dd45ec-c2ac-4a47-8b71-986095f25450");

	public void setup()
	{
		if (adapter == null)
			return;

		if (!adapter.isEnabled())
		{
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			parent.startActivityForResult(i, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					setup();
				}
			});
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			parent.requestPermissionForResult(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					setup();
				}
			});
			return;
		}

		discoverer = new BluetoothDiscoverer(this);
		slave = new BluetoothSlave(this);
		master=  new BluetoothMaster(this);

		isSetup = true;
	}

	public void cleanUp()
	{
		discoverer.cleanUp();
		slave.cleanUp();
		master.cleanUp();
	}

	public BluetoothMaster getMaster()
	{
		return master;
	}

	public BluetoothSlave getSlave()
	{
		return slave;
	}

	public BluetoothDiscoverer getDiscoverer()
	{
		return discoverer;
	}

	public boolean getIsSetup()
	{
		return isSetup;
	}

	public MainActivity getParent()
	{
		return parent;
	}

	public BluetoothAdapter getAdapter()
	{
		return adapter;
	}

	public static class BluetoothNotSupportedException extends Exception {}
	public static class BluetoothDiscoveryFailedException extends Exception {}
}

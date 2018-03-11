package com.munger.stereocamera.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Set;
import java.util.UUID;

public class BluetoothCtrl
{
	public BluetoothCtrl(MyApplication parent)
	{
		this.parent = parent;
		adapter = BluetoothAdapter.getDefaultAdapter();
	}

	private BluetoothAdapter adapter;
	private MyApplication parent;
	private boolean isSetup = false;
	private final Object lock = new Object();

	private BluetoothDiscoverer discoverer;
	private BluetoothSlave slave;
	private BluetoothMaster master;


	public static int LISTEN_TIMEOUT = 30000;
	public static int DISCOVER_TIMEOUT = 300000;


	public static final UUID APP_ID = UUID.fromString("e1dd45ec-c2ac-4a47-8b71-986095f25450");

	public interface SetupListener
	{
		void onSetup();
	}

	public void setup(final SetupListener listener)
	{
		if (adapter == null)
			return;

		BaseActivity a = MyApplication.getInstance().getCurrentActivity();

		if (!adapter.isEnabled())
		{
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			a.startActivityForResult(i, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					setup(listener);
				}
			});
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			a.requestPermissionForResult(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					setup(listener);
				}
			});
			return;
		}

		discoverer = new BluetoothDiscoverer(this);

		isSetup = true;
		listener.onSetup();
	}

	public interface ConnectListener
	{
		void onFailed();
		void onDiscoverable();
		void onConnected();
		void onDisconnected();
	}

	public void connect(BluetoothDevice device, ConnectListener listener)
	{
		synchronized (lock)
		{
			if (master != null)
				master.cleanUp();
		}

		master = new BluetoothMaster(this, device, listener);
	}

	public boolean isMasterConnected()
	{
		synchronized (lock)
		{
			if (master == null)
				return false;

			return master.isConnected();
		}
	}

	public void listen(boolean startDiscovery, ConnectListener listener)
	{
		cancelListen();
		slave = new BluetoothSlave(this, startDiscovery, LISTEN_TIMEOUT, listener);
	}

	public void cancelListen()
	{
		synchronized (lock)
		{
			if (slave != null)
				slave.cleanUp();

			slave = null;
		}
	}

	public void discover(BluetoothDiscoverer.DiscoverListener listener) throws BluetoothDiscoveryFailedException
	{
		cancelDiscover();
		discoverer.discover(listener, DISCOVER_TIMEOUT);
	}

	public void cancelDiscover()
	{
		synchronized (lock)
		{
			if (discoverer.isDiscovering())
				discoverer.cancelDiscover();
		}
	}

	public void cleanUp()
	{
		discoverer.cleanUp();
		slave.cleanUp();

		if (master != null)
		{
			master.cleanUp();
			master = null;
		}
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

	public MyApplication getParent()
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

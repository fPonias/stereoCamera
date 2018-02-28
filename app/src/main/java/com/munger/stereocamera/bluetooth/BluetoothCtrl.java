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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

	public enum Roles
	{
		MASTER,
		SLAVE,
		NONE
	};

	private static final String LAST_ROLE_KEY = "lastRole";
	private static final String LAST_CLIENT_KEY = "lastClient";

	private SharedPreferences preferences;
	private Roles lastRole = Roles.NONE;
	private String lastClient = null;

	public static final UUID APP_ID = UUID.fromString("e1dd45ec-c2ac-4a47-8b71-986095f25450");

	public interface SetupListener
	{
		void onSetup();
	}

	public void setup(final SetupListener listener)
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
					setup(listener);
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
					setup(listener);
				}
			});
			return;
		}

		discoverer = new BluetoothDiscoverer(this);
		slave = new BluetoothSlave(this);
		master = new BluetoothMaster(this);

		readPreferences();

		isSetup = true;
		listener.onSetup();
	}

	private void readPreferences()
	{
		preferences = MainActivity.getInstance().getPreferences(Context.MODE_PRIVATE);

		if (preferences.contains(LAST_ROLE_KEY))
		{
			int value = preferences.getInt(LAST_ROLE_KEY, -1);
			lastRole = Roles.values()[value];
		}
		else
		{
			lastRole = Roles.NONE;
		}

		if (preferences.contains(LAST_CLIENT_KEY))
		{
			String value = preferences.getString(LAST_CLIENT_KEY, null);
			lastClient = value;
		}
		else
		{
			lastClient = null;
		}
	}

	public Roles getLastRole()
	{
		return lastRole;
	}

	public void setLastRole(Roles role)
	{
		preferences.edit().putInt(LAST_ROLE_KEY, role.ordinal()).apply();
	}

	public String getLastClient()
	{
		return lastClient;
	}

	public void setLastClient(String client)
	{
		preferences.edit().putString(LAST_CLIENT_KEY, client).apply();
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

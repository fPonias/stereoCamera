package com.munger.stereocamera.ip.bluetooth;

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
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketCtrlCtrl;
import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.slave.SlaveComm;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Set;
import java.util.UUID;

public class BluetoothCtrl implements SocketCtrlCtrl
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


	public static int LISTEN_TIMEOUT = 30000;
	public static int DISCOVER_TIMEOUT = 300000;


	public static final UUID APP_ID = UUID.fromString("e1dd45ec-c2ac-4a47-8b71-986095f25450");



	public void setup(final IPListeners.SetupListener listener)
	{
		if (adapter == null)
			return;

		if (!adapter.isEnabled())
		{
			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			parent.startActivityForResult(i, new MainActivity.ActivityResultListener()
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
			parent.requestPermissionForResult(Manifest.permission.ACCESS_COARSE_LOCATION, new MainActivity.PermissionResultListener()
			{
				@Override
				public void onResult(int resultCode)
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



	public void connect(BluetoothDevice device, IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (master == null)
				master = new BluetoothMaster(this);
		}

		master.connect(device, listener);
	}

	public void cancelConnect()
	{
		synchronized (lock)
		{
			if (master != null)
				master.cleanUp();

			master = null;
		}
	}

	public void listen(boolean startDiscovery, IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (slave == null)
				slave = new BluetoothSlave(this);
		}

		slave.start(startDiscovery, LISTEN_TIMEOUT, listener);
	}

	public void cancelListen()
	{
		synchronized (lock)
		{
			if (slave != null)
				slave.cleanUp();
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
			if (discoverer != null && discoverer.isDiscovering())
				discoverer.cancelDiscover();
		}
	}

	public void cleanUp()
	{
		if (discoverer != null)
		{
			discoverer.cleanUp();
			discoverer = null;
		}

		if (slave != null)
		{
			slave.cleanUp();
			slave = null;
		}

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

	@Override
	public MasterComm getMasterComm()
	{
		return master.getComm();
	}

	@Override
	public SlaveComm getSlaveComm()
	{
		return slave.getComm();
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


	public Set<BluetoothDevice> getKnownDevices()
	{
		Set<BluetoothDevice> devices = getAdapter().getBondedDevices();
		return devices;
	}

	public BluetoothDevice getKnownDeviceByAddress(String address)
	{
		Set<BluetoothDevice> devices = getAdapter().getBondedDevices();

		for(BluetoothDevice device : devices)
		{
			if (device.getAddress().equals(address))
				return device;
		}

		return null;
	}

	public BluetoothDevice getKnownDevice(String id)
	{
		Set<BluetoothDevice> devices = getAdapter().getBondedDevices();

		for(BluetoothDevice device : devices)
		{
			if (device.getAddress().equals(id))
				return device;
		}

		return null;
	}
}

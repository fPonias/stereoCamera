package com.munger.stereocamera.ip.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.munger.stereocamera.BaseFragment;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.SocketCtrlCtrl;
import com.munger.stereocamera.ip.command.CommCtrl;

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
	private CommCtrl commCtrl;


	public static int LISTEN_TIMEOUT = 30000;
	public static int DISCOVER_TIMEOUT = 300000;


	public static final UUID APP_ID = UUID.fromString("e1dd45ec-c2ac-4a47-8b71-986095f25450");



	public void setup(final IPListeners.SetupListener listener)
	{
		if (adapter == null)
			return;

		if (!adapter.isEnabled())
		{
			parent.enableBluetoothForResult(i -> {
				setup(listener);
			});

			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(parent, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			parent.requestPermissionForResult(Manifest.permission.ACCESS_COARSE_LOCATION, result -> {
				setup(listener);
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
		listen(startDiscovery, LISTEN_TIMEOUT, listener);
	}

	public void listen(boolean startDiscovery, long timeout, IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (slave == null)
				slave = new BluetoothSlave(this);
		}

		slave.start(startDiscovery, timeout, listener);
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

	public BluetoothMaster getSlave()
	{
		return master;
	}

	public BluetoothSlave getMaster()
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

	public Boolean isMaster()
	{
		if (!isSetup)
			return null;

		if (master != null)
			return true;
		else
			return false;
	}
}

package com.munger.stereocamera.ip.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.Set;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class BluetoothDiscoverer {
	public BluetoothDiscoverer(BluetoothCtrl parent) {
		this.parent = parent;
		parent.getParent().registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
	}

	public void cleanUp() {
		parent.getParent().unregisterReceiver(receiver);
		cancelDiscover();
	}

	private BluetoothCtrl parent;
	private DiscoverListener discoverListener = null;
	private final Object lock = new Object();
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			handleReceive(intent);
		}
	};

	private void handleReceive(Intent i) {
		String action = i.getAction();

		if (!action.equals(BluetoothDevice.ACTION_FOUND))
			return;

		BluetoothDevice device = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		sendDevice(device);
	}

	private void sendDevice(BluetoothDevice device) {
		synchronized (lock) {
			if (discoverListener == null)
				return;

			discoverListener.onDiscovered(device);
		}
	}

	private void sendKnown(BluetoothDevice device) {
		synchronized (lock) {
			if (discoverListener == null)
				return;

			discoverListener.onKnown(device);
		}
	}

	public interface DiscoverListener {
		void onKnown(BluetoothDevice device);

		void onDiscovered(BluetoothDevice device);
	}

	public boolean isDiscovering() {
		synchronized (lock) {
			return (discoverListener != null) ? true : false;
		}
	}

	void discover(DiscoverListener listener, final long timeout) throws BluetoothCtrl.BluetoothDiscoveryFailedException, SecurityException {
		synchronized (lock) {
			if (discoverListener != null)
				return;

			discoverListener = listener;
		}

		//parent.getAdapter().cancelDiscovery();
		boolean success = parent.getAdapter().startDiscovery();

		if (!success)
		{
			synchronized (lock)
			{
				discoverListener = null;
			}

			throw new BluetoothCtrl.BluetoothDiscoveryFailedException();
		}

		Thread t = new Thread(new Runnable() { public void run()
		{
			try {Thread.sleep(timeout);} catch(InterruptedException e){}

			parent.getAdapter().cancelDiscovery();

			synchronized (lock)
			{
				discoverListener = null;
			}
		}});
		t.start();

		Set<BluetoothDevice> devices = parent.getAdapter().getBondedDevices();
		for (BluetoothDevice device : devices)
		{
			sendKnown(device);
		}
	}

	public void cancelDiscover()
	{
		parent.getAdapter().cancelDiscovery();

		synchronized (lock)
		{
			if(discoverListener == null)
				return;

			discoverListener = null;
		}
	}
}

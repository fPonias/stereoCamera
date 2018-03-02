package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.munger.stereocamera.MainActivity;

import java.io.IOException;
import java.net.ConnectException;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class BluetoothMaster
{
	public BluetoothMaster(BluetoothCtrl parent, BluetoothDevice device, BluetoothCtrl.ConnectListener listener)
	{
		this.server = parent;
		this.connectListener = listener;
		this.targetDevice = device;

		connect();
	}

	private BluetoothCtrl server;
	private BluetoothDevice targetDevice;
	private BluetoothSocket targetSocket;
	private Thread connectThread;
	private BluetoothCtrl.ConnectListener connectListener;
	private final Object lock = new Object();

	private BluetoothMasterComm masterComm;

	public BluetoothMasterComm getComm()
	{
		return masterComm;
	}

	public BluetoothSocket getSocket()
	{
		return targetSocket;
	}

	public boolean isConnected()
	{
		synchronized (lock)
		{
			if (connectListener == null)
				return false;

			return targetSocket.isConnected();

		}
	}

	private void connect()
	{
		connectThread = new Thread(new Runnable() { public void run()
		{
			connect2();
		}});
		connectThread.start();
	}

	private void connect2()
	{
		try
		{
			targetSocket = targetDevice.createRfcommSocketToServiceRecord(BluetoothCtrl.APP_ID);

			targetSocket.connect();
		}
		catch(ConnectException ce)
		{
			try
			{
				targetSocket.close();
			}
			catch(IOException e){}

			synchronized (lock)
			{
				if (connectListener != null)
					connectListener.onFailed();

				return;
			}
		}
		catch(IOException e){
			synchronized (lock)
			{
				if (connectListener != null)
					connectListener.onFailed();

				return;
			}
		}

		synchronized (lock)
		{
			if (connectListener == null)
				return;
		}

		masterComm = new BluetoothMasterComm(server);
		connectListener.onConnected();
	}

	public void cleanUp()
	{
		try
		{
			targetSocket.close();

			if (masterComm != null)
				masterComm.cleanUp();
		}
		catch(IOException e){}

		connectListener.onDisconnected();
	}
}

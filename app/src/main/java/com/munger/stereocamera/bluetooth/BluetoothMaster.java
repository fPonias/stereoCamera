package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.net.ConnectException;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class BluetoothMaster
{
	public BluetoothMaster(BluetoothCtrl parent)
	{
		this.server = parent;
	}

	public interface ConnectListener
	{
		void onFailed();
		void onConnected();
	}

	private BluetoothCtrl server;
	private BluetoothDevice targetDevice;
	private BluetoothSocket targetSocket;
	private Thread connectThread;
	private ConnectListener connectListener;
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

	public void connect(BluetoothDevice device, ConnectListener listener)
	{
		synchronized (lock)
		{
			if (connectListener != null)
				return;

			connectListener = listener;
		}

		targetDevice = device;

		connectThread = new Thread(new Runnable()
		{
			@Override
			public void run()
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
		});
		connectThread.start();
	}

	public void cancelConnect()
	{
		synchronized (lock)
		{
			if (connectListener == null)
				return;

			connectListener = null;
		}

		try
		{
			targetSocket.close();
		}
		catch(IOException e){}
	}

	public void cleanUp()
	{
		cancelConnect();

		if (masterComm != null)
			masterComm.cleanUp();
	}
}

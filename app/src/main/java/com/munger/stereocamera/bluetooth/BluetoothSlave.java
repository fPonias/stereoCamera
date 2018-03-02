package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;

import java.io.IOException;

public class BluetoothSlave
{
	public BluetoothSlave(BluetoothCtrl server, boolean doDiscovery, long timeout, BluetoothCtrl.ConnectListener listener)
	{
		this.server = server;
		this.timeout = timeout;
		listenListener = listener;

		isCancelled = false;

		if (doDiscovery)
			startDiscovery();
		else
			startListener();
	}

	private BluetoothCtrl server;
	private BluetoothCtrl.ConnectListener listenListener;
	private boolean isCancelled;
	private final Object lock = new Object();
	private BluetoothServerSocket serverSocket;
	private BluetoothSocket socket;
	private long timeout;

	private BluetoothSlaveComm slaveComm;

	public BluetoothSocket getSocket()
	{
		return socket;
	}
	public BluetoothSlaveComm getComm() { return slaveComm; }

	private void startDiscovery()
	{
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, timeout / 1000);

		MyApplication.getInstance().getCurrentActivity().startActivityForResult(discoverableIntent, new MainActivity.ResultListener()
		{
			@Override
			public void onResult(int resultCode, Intent data)
			{
			listen();
			}
		});
	}

	private void startListener()
	{
		listen();
	}

	private void listen()
	{
		Thread listenThread = new Thread(new Runnable() { public void run()
		{
			doListen();
		}});
		listenThread.start();

		Thread cancelThread = new Thread(new Runnable() { public void run()
		{
			try {Thread.sleep(timeout);} catch(InterruptedException e){}
			doCancel();
		}});
		cancelThread.start();
	}

	private void doCancel()
	{
		boolean doCleanUp = false;
		synchronized (lock)
		{
			if (serverSocket == null || socket == null)
				doCleanUp = true;
			else if (socket != null && !socket.isConnected())
				doCleanUp = true;
		}

		if (doCleanUp)
		{
			listenListener.onFailed();
			cleanUp();
		}
	}

	private void doListen()
	{
		try
		{
			serverSocket = server.getAdapter().listenUsingRfcommWithServiceRecord(MyApplication.BT_SERVICE_NAME, BluetoothCtrl.APP_ID);
			socket = serverSocket.accept();
			synchronized (lock)
			{
				if (isCancelled)
				{
					if (socket != null && serverSocket != null)
					{
						try
						{
							serverSocket.close();
						}
						catch(IOException e){}

						socket = null;
						serverSocket = null;
					}
				}
			}
		}
		catch(IOException e){
			Log.d(MyApplication.BT_SERVICE_NAME, "failed to run bluetooth socket");

			synchronized (lock)
			{
				if (isCancelled)
					return;
			}

			listenListener.onFailed();
		}

		if (socket != null)
		{
			slaveComm = new BluetoothSlaveComm(server);
			listenListener.onConnected();
		}
		else
		{
			socket = null;
			listenListener.onFailed();
		}
	}

	public void cleanUp()
	{
		synchronized (lock)
		{
			if (isCancelled)
				return;

			isCancelled = true;
		}

		try
		{
			if (serverSocket != null)
				serverSocket.close();
		}
		catch(IOException e){
			Log.d(MyApplication.BT_SERVICE_NAME, "failed to close bluetooth socket");
		}

		if (slaveComm != null)
			slaveComm.cleanUp();
	}
}

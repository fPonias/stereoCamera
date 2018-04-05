package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;

import java.io.IOException;

public class BluetoothSlave
{
	public BluetoothSlave(BluetoothCtrl server)
	{
		this.server = server;
	}

	public void start(boolean doDiscovery, long timeout, BluetoothCtrl.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (listenThread != null)
			{
				cleanUp();
			}
		}

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
		//timeout isn't working ...
		//discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, timeout);

		MainActivity.getInstance().startActivityForResult(discoverableIntent, new MainActivity.ActivityResultListener()
		{
			@Override
			public void onResult(int resultCode, Intent data)
			{
				if (resultCode == 0)
					listenListener.onFailed();
				else
				{
					listenListener.onDiscoverable();
					listen();
				}
			}
		});
	}

	public void clearListener()
	{
		synchronized (listenLock)
		{
			listenListener = new BluetoothCtrl.ConnectListener()
			{
				public void onFailed() {}
				public void onDiscoverable() {}
				public void onConnected() {}
				public void onDisconnected() {}
			};
		}
	}

	private String getTag() { return "Bluetooth Slave";}

	private void startListener()
	{
		listen();
	}

	private Thread listenThread = null;
	private Thread cancelThread = null;
	private final Object listenLock = new Object();

	private void listen()
	{
		synchronized (listenLock)
		{
			if (listenThread != null)
				return;

			listenThread = new Thread(new Runnable() { public void run()
			{
				doListen();

				synchronized (listenLock)
				{
					listenThread = null;
					cancelThread = null;
					listenLock.notify();
				}
			}});

			cancelThread = new Thread(new Runnable() { public void run()
			{
				boolean runCancel = false;

				synchronized (listenLock)
				{
					try {listenLock.wait(timeout);} catch(InterruptedException e){}

					Log.d(MainActivity.BT_SERVICE_NAME, "checking bluetooth cancel");
					runCancel = (cancelThread != null);

					listenThread = null;
					cancelThread = null;
				}

				if (runCancel)
				{
					Log.d(MainActivity.BT_SERVICE_NAME, "failed to run bluetooth socket, cancelling");
					doCancel();
				}
				else
					Log.d(MainActivity.BT_SERVICE_NAME, "no bluetooth cancel");

			}});
		}

		listenThread.start();
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

	private BluetoothDevice lastConnected = null;

	public BluetoothDevice getLastConnected()
	{
		return lastConnected;
	}

	public void clearLastConnected()
	{
		lastConnected = null;
	}

	private void doListen()
	{
		try
		{
			Log.d(getTag(), "starting server socket");
			serverSocket = server.getAdapter().listenUsingRfcommWithServiceRecord(MainActivity.BT_SERVICE_NAME, BluetoothCtrl.APP_ID);

			lastConnected = null;

			Log.d(getTag(), "starting bluetooth socket");
			socket = serverSocket.accept();
			Log.d(getTag(), "bluetooth socket connected?");
			synchronized (lock)
			{
				if (isCancelled)
				{
					Log.d(getTag(), "bluetooth socket not connected");
					cleanUp();
				}
			}

			cleanUpServerSocket();
		}
		catch(IOException e){
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to run bluetooth socket");

			synchronized (lock)
			{
				if (isCancelled)
					return;
			}

			listenListener.onFailed();
		}

		if (socket != null)
		{
			Log.d(getTag(), "bluetooth socket connected");
			lastConnected = socket.getRemoteDevice();
			slaveComm = new BluetoothSlaveComm(server);
			listenListener.onConnected();
		}
		else
		{
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to run bluetooth socket");
			cleanUp();
			listenListener.onFailed();
		}
	}

	public boolean isConnected()
	{
		if (slaveComm != null)
			return true;
		else
			return false;
	}

	public void cleanUp()
	{
		synchronized (lock)
		{
			if (isCancelled)
				return;

			isCancelled = true;
		}

		if (slaveComm != null)
		{
			slaveComm.cleanUp();
			slaveComm = null;
		}

		if (socket != null)
		{
			try
			{
				socket.close();
			}
			catch(IOException e){}

			socket = null;
		}

		cleanUpServerSocket();
	}

	private void cleanUpServerSocket()
	{
		if (serverSocket == null)
			return;

		try
		{
			serverSocket.close();
		}
		catch (IOException e)
		{
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to close bluetooth socket");
		}

		serverSocket = null;
	}
}

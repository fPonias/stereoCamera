package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import com.munger.stereocamera.MainActivity;

import java.io.IOException;

public class BluetoothSlave
{
	public BluetoothSlave(BluetoothCtrl server)
	{
		this.server = server;
	}

	public interface ListenListener
	{
		void onAttached();
		void onFailed();
	}

	private BluetoothCtrl server;
	private ListenListener listenListener = null;
	private final Object lock = new Object();
	private Thread listenThread;
	private BluetoothServerSocket serverSocket;
	private BluetoothSocket socket;
	private long timeout;

	private BluetoothSlaveComm slaveComm;

	public BluetoothSocket getSocket()
	{
		return socket;
	}
	public BluetoothSlaveComm getComm() { return slaveComm; }

	public boolean isListening()
	{
		synchronized (lock)
		{
			return (listenListener != null) ? true : false;
		}
	}

	public void startDiscovery(ListenListener listener, long timeout)
	{
		synchronized (lock)
		{
			if (listenListener != null)
				return;

			listenListener = listener;
			this.timeout = timeout;
		}

		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, timeout / 1000);

		server.getParent().startActivityForResult(discoverableIntent, new MainActivity.ResultListener()
		{
			@Override
			public void onResult(int resultCode, Intent data)
			{
				listen();
			}
		});
	}

	public void startListener(ListenListener listener, long timeout)
	{
		synchronized (lock)
		{
			if (listenListener != null)
				return;

			listenListener = listener;
			this.timeout = timeout;
		}

		listen();
	}

	private void listen()
	{
		listenThread = new Thread(new Runnable() { public void run()
		{
			doListen();
		}});
		listenThread.start();

		Thread t = new Thread(new Runnable() { public void run()
		{
			try {Thread.sleep(timeout);} catch(InterruptedException e){}

			boolean doCancel = false;
			synchronized (lock)
			{
				if (socket == null)
					doCancel = true;
			}

			if (doCancel)
			{
				listenListener.onFailed();
				cancelListen();
			}
		}});
		t.start();
	}

	private void doListen()
	{
		try
		{
			serverSocket = server.getAdapter().listenUsingRfcommWithServiceRecord(MainActivity.BT_SERVICE_NAME, BluetoothCtrl.APP_ID);
			synchronized (lock)
			{
				if (listenListener == null)
				{
					serverSocket = null;
					return;
				}
			}

			socket = serverSocket.accept();
			synchronized (lock)
			{
				if (listenListener == null)
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
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to run bluetooth socket");

			if (listenListener == null)
				return;

			listenListener.onFailed();
			synchronized (lock)
			{
				listenListener = null;
				serverSocket = null;
				socket = null;
				return;
			}
		}

		if (socket != null)
		{
			slaveComm = new BluetoothSlaveComm(server);
			listenListener.onAttached();
		}
		else
		{
			listenListener.onFailed();
		}
	}

	public void cancelListen()
	{
		BluetoothServerSocket doClose = null;

		synchronized (lock)
		{
			if (listenListener == null)
				return;

			listenListener = null;

			if (socket != null)
				doClose = serverSocket;

			socket = null;
			serverSocket = null;
		}

		if (doClose != null)
		{
			try
			{
				doClose.close();
			}
			catch(IOException e){
				Log.d(MainActivity.BT_SERVICE_NAME, "failed to close bluetooth socket");
			}
		}
	}

	public void cleanUp()
	{
		cancelListen();

		if (slaveComm != null)
		{
			slaveComm.cleanUp();
		}
	}
}

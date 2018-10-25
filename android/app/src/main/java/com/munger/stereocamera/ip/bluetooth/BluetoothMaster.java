package com.munger.stereocamera.ip.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketFactory;
import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.utility.RemoteState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class BluetoothMaster implements SocketCtrl
{
	public BluetoothMaster(BluetoothCtrl parent)
	{
		this.server = parent;
	}

	private BluetoothCtrl server;
	private BluetoothDevice targetDevice;
	private BluetoothSocket socket;
	private Thread connectThread;
	private IPListeners.ConnectListener connectListener;
	private final Object lock = new Object();

	private Comm masterComm;

	public Comm getComm()
	{
		return masterComm;
	}

	public Socket getSocket()
	{
		try
		{
			return SocketFactory.getSocket(socket);
		}
		catch (SocketFactory.InvalidClassException e){
			return null;
		}
	}

	public boolean isConnected()
	{
		synchronized (lock)
		{
			if (connectListener == null)
				return false;

			if (socket == null)
				return false;

			return socket.isConnected();

		}
	}

	public void connect(BluetoothDevice device, IPListeners.ConnectListener listener)
	{
		if (targetDevice != null && socket != null)
		{
			if (!targetDevice.getAddress().equals(device.getAddress()))
				cleanUp();
		}

		this.connectListener = listener;
		this.targetDevice = device;

		connectThread = new Thread(new Runnable() { public void run()
		{
			connect2();
			connectThread = null;
		}});
		connectThread.start();
	}

	private void connect2()
	{
		try
		{
			socket = targetDevice.createRfcommSocketToServiceRecord(BluetoothCtrl.APP_ID);

			socket.connect();
			masterComm = new Comm(this);
		}
		catch (ConnectException ce)
		{
			try
			{
				socket.close();
				socket = null;
			}
			catch (IOException e)
			{
			}

			synchronized (lock)
			{
				if (connectListener != null)
					connectListener.onFailed();

				return;
			}
		}
		catch (IOException e)
		{
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

		connectListener.onConnected();
	}

	public void cleanUp()
	{
		try
		{
			if (masterComm != null)
			{
				masterComm = null;
			}

			if (socket != null)
			{
				socket.close();
				socket = null;
			}
		}
		catch(IOException e){}

		connectListener.onDisconnected();
	}
}

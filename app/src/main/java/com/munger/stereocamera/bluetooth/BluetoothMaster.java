package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.bluetooth.command.master.commands.Ping;
import com.munger.stereocamera.bluetooth.utility.RemoteState;
import com.munger.stereocamera.bluetooth.utility.TimedCommand;

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

	private BluetoothCtrl server;
	private BluetoothDevice targetDevice;
	private BluetoothSocket targetSocket;
	private RemoteState remoteState;
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

			if (targetSocket == null)
				return false;

			return targetSocket.isConnected();

		}
	}

	public void connect(BluetoothDevice device, BluetoothCtrl.ConnectListener listener)
	{
		if (targetDevice != null && targetSocket != null)
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
			targetSocket = targetDevice.createRfcommSocketToServiceRecord(BluetoothCtrl.APP_ID);

			targetSocket.connect();
		}
		catch (ConnectException ce)
		{
			try
			{
				targetSocket.close();
				targetSocket = null;
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

		masterComm = new BluetoothMasterComm(server);
		remoteState = new RemoteState(masterComm);

		connectListener.onConnected();
	}

	public RemoteState getRemoteState()
	{
		return remoteState;
	}

	public void cleanUp()
	{
		try
		{
			if (masterComm != null)
			{
				masterComm.cleanUp();
				masterComm = null;
			}

			if (targetSocket != null)
			{
				targetSocket.close();
				targetSocket = null;
			}
		}
		catch(IOException e){}

		connectListener.onDisconnected();
	}
}

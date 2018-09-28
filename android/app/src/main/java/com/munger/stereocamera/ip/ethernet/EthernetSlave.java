package com.munger.stereocamera.ip.ethernet;

import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketFactory;
import com.munger.stereocamera.ip.command.slave.SlaveComm;
import com.munger.stereocamera.ip.utility.RemoteState;

import java.io.IOException;
import java.net.ServerSocket;

public class EthernetSlave implements SocketCtrl
{
	@Override
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

	private Object listenLock = new Object();
	private Object lock = new Object();
	private Thread listenThread;
	private Thread cancelThread;
	private int timeout;
	private int targetPort;
	private ServerSocket serverSocket;
	private java.net.Socket socket;
	private IPListeners.ConnectListener listenListener;
	private boolean isCancelled;
	private SlaveComm slaveComm;

	public void listen(final int port)
	{
		synchronized (listenLock)
		{
			if (listenThread != null)
				return;

			listenThread = new Thread(new Runnable() { public void run()
			{
				targetPort = port;
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

	private String getTag()
	{
		return "EthernetSlave";
	}

	private void doListen()
	{
		try
		{
			Log.d(getTag(), "starting server socket");
			serverSocket = new ServerSocket(targetPort);

			Log.d(getTag(), "starting ethernet socket");
			socket = serverSocket.accept();
			Log.d(getTag(), "ethernet socket connected?");
			synchronized (lock)
			{
				if (isCancelled)
				{
					Log.d(getTag(), "ethernet socket not connected");
					cleanUp();
				}
			}

			cleanUpServerSocket();
		}
		catch(IOException e){
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to run ethernet socket");

			synchronized (lock)
			{
				if (isCancelled)
					return;
			}

			listenListener.onFailed();
		}

		if (socket != null)
		{
			Log.d(getTag(), "ethernet socket connected");
			slaveComm = new SlaveComm(this);
			listenListener.onConnected();
		}
		else
		{
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to run ethernet socket");
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

	@Override
	public RemoteState getRemoteState()
	{
		return null;
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
			Log.d(MainActivity.BT_SERVICE_NAME, "failed to close ethernet socket");
		}

		serverSocket = null;
	}
}

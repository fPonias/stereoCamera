package com.munger.stereocamera.ip.ethernet;

import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketFactory;
import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.utility.RemoteState;

import java.io.IOException;
import java.net.ServerSocket;

public class EthernetMaster implements SocketCtrl
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
	private Comm slaveComm;


	public EthernetMaster(IPListeners.ConnectListener listener)
	{
		super();
		this.listenListener = listener;
	}

	public void setConnectListener(IPListeners.ConnectListener listener)
	{
		this.listenListener = listener;
	}

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

					Log.d("stereoCamera", "checking bluetooth cancel");
					runCancel = (cancelThread != null);

					listenThread = null;
					cancelThread = null;
				}

				if (runCancel)
				{
					Log.d("stereoCamera", "failed to run bluetooth socket, cancelling");
					doCancel();
				}
				else
					Log.d("stereoCamera", "no bluetooth cancel");

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
		return "EthernetMaster";
	}

	private void doListen()
	{
		try
		{
			Log.d("stereoCamera", "starting server socket");
			serverSocket = new ServerSocket(targetPort);

			Log.d("stereoCamera", "starting ethernet socket");
			socket = serverSocket.accept();
			Log.d("stereoCamera", "ethernet socket connected?");
			synchronized (lock)
			{
				if (isCancelled)
				{
					Log.d("stereoCamera", "ethernet socket not connected");
					cleanUp();
				}
			}

			cleanUpServerSocket();

			if (socket != null)
			{
				Log.d("stereoCamera", "ethernet socket connected");
				slaveComm = new Comm(this);
				listenListener.onConnected();
			}
			else
			{
				Log.d("stereoCamera", "failed to run ethernet socket");
				cleanUp();
				listenListener.onFailed();
			}
		}
		catch(IOException e){
			Log.d("stereoCamera", "failed to run ethernet socket");

			synchronized (lock)
			{
				if (isCancelled)
					return;
			}

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
			Log.d("stereoCamera", "failed to close ethernet socket");
		}

		serverSocket = null;
	}
}

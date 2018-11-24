package com.munger.stereocamera.ip.ethernet;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketFactory;
import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.utility.RemoteState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class EthernetSlave implements SocketCtrl
{
	private String targetAddress = null;
	private int targetPort;
	private Socket socket;
	private IPListeners.ConnectListener connectListener;
	private Thread connectThread;
	private Object lock = new Object();
	private Comm masterComm;
	private RemoteState remoteState;

	public void connect(String address, int port, IPListeners.ConnectListener listener)
	{
		if (targetAddress != null && socket != null)
		{
			if (!address.equals(targetAddress) || targetPort != port)
				cleanUp();
		}

		this.connectListener = listener;
		this.targetPort = port;
		this.targetAddress = address;

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
			byte[] bytes = getUTF8Bytes(targetAddress);
			InetAddress addr = InetAddress.getByAddress(bytes);
			socket = new Socket(addr, targetPort);
			masterComm = new Comm(this);
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

		remoteState = new RemoteState(MyApplication.getInstance().getCtrl());

		connectListener.onConnected();
	}

	public static boolean checkIP(String ip)
	{
		try
		{
			byte[] bytes = getUTF8Bytes(ip);
			InetAddress addr = InetAddress.getByAddress(bytes);
		}
		catch(UnknownHostException e){
			return false;
		}

		return true;
	}

	public static byte[] getUTF8Bytes(String str)
	{
		String[] parts = str.split("\\.");

		if (parts.length != 4)
			return null;

		byte[] ret = new byte[4];
		for (int i = 0; i < 4; i++)
		{
			String part = parts[i];
			int num = -1;

			try
			{
				num = Integer.parseInt(part);
			}
			catch(NumberFormatException e){
				return null;
			}

			if (num < 0 || num > 255)
				return null;

			ret[i] = (byte) num;
		}

		return ret;
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

	@Override
	public com.munger.stereocamera.ip.Socket getSocket()
	{
		try
		{
			return SocketFactory.getSocket(socket);
		}
		catch (SocketFactory.InvalidClassException e){
			return null;
		}
	}

	@Override
	public boolean isConnected()
	{
		return (socket != null) ? socket.isConnected() : false;
	}
}

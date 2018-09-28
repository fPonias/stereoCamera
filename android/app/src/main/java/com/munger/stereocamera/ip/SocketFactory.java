package com.munger.stereocamera.ip;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SocketFactory
{
	public static Socket getSocket(Object target) throws InvalidClassException
	{
		if (target == null)
			return null;

		if (target instanceof BluetoothSocket)
		{
			return new BluetoothSocketWrapper((BluetoothSocket) target);
		}
		else if (target instanceof java.net.Socket)
		{
			return new EthernetSocketWrapper((java.net.Socket) target);
		}

		throw new InvalidClassException("no defined wrapper for " + target.getClass().getName());
	}

	public static class InvalidClassException extends Exception
	{
		public InvalidClassException(String reason)
		{
			super(reason);
		}
	}

	public static class BluetoothSocketWrapper implements Socket
	{
		public BluetoothSocket target;

		public BluetoothSocketWrapper(BluetoothSocket target)
		{
			this.target = target;
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			return target.getInputStream();
		}

		@Override
		public OutputStream getOutputStream() throws IOException
		{
			return target.getOutputStream();
		}

		@Override
		public boolean isConnected()
		{
			return target.isConnected();
		}
	}

	public static class EthernetSocketWrapper implements Socket
	{
		public java.net.Socket target;

		public EthernetSocketWrapper(java.net.Socket target)
		{
			this.target = target;
		}


		@Override
		public InputStream getInputStream() throws IOException
		{
			return target.getInputStream();
		}

		@Override
		public OutputStream getOutputStream() throws IOException
		{
			return target.getOutputStream();
		}

		@Override
		public boolean isConnected()
		{
			return target.isConnected();
		}
	}
}

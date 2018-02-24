package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothSocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothMasterComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;
	private InputStream ins;
	private OutputStream outs;
	private byte[] buffer;
	private static final int BUFFER_SIZE = 4096;

	public BluetoothMasterComm(BluetoothCtrl ctrl)
	{
		this.ctrl = ctrl;
		this.socket = ctrl.getMaster().getSocket();

		try
		{
			ins = socket.getInputStream();
			outs = socket.getOutputStream();
		}
		catch (IOException e){

		}

		buffer = new byte[BUFFER_SIZE];
	}

	public interface PingListener
	{
		void pong(long diff);
		void fail();
	}

	public void ping(final PingListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			try
			{
				ping2(listener);
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	private void ping2(PingListener listener) throws IOException
	{
		long now = System.currentTimeMillis();
		byte[] bytes = ByteBuffer.allocate(4).putInt(BluetoothComm.PING).array();
		outs.write(bytes);

		int read;

		do
		{
			read = ins.read(buffer);
		} while (read == 0);

		if (read < 4)
		{
			listener.fail();
			return;
		}


		ByteBuffer bb = ByteBuffer.wrap(buffer);
		int action = bb.getInt();

		if (action == BluetoothComm.PING)
		{
			long then = System.currentTimeMillis();
			long diff = then - now;

			listener.pong(diff);
		}
		else
		{
			listener.fail();
		}
	}

	public interface ShutterListener
	{
		void onData(byte[] data);
		void fail();
	}

	public void shutter(final ShutterListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			try
			{
				shutter2(listener);
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	private void shutter2(ShutterListener listener) throws IOException
	{
		long now = System.currentTimeMillis();
		byte[] bytes = ByteBuffer.allocate(4).putInt(BluetoothComm.FIRE_SHUTTER).array();
		outs.write(bytes);

		int read;

		do
		{
			read = ins.read(buffer);
		} while (read == 0);

		if (read < 4)
		{
			listener.fail();
			return;
		}


		ByteBuffer bb = ByteBuffer.wrap(buffer);
		int action = bb.getInt();

		if (action != BluetoothComm.FIRE_SHUTTER)
		{
			listener.fail();
			return;
		}

		long then = System.currentTimeMillis();

		int size = bb.getInt();
		int sz = read - 8;
		ByteBuffer imgBuffer = ByteBuffer.allocate(size);
		imgBuffer.put(buffer, 8, sz);

		while (sz < size)
		{
			read = ins.read(buffer);
			sz += read;
			imgBuffer.put(buffer, 0 , read);
		}

		listener.onData(imgBuffer.array());
	}

	public void cleanUp()
	{
	}
}

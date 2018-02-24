package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothSocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothSlaveComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;
	private InputStream ins;
	private OutputStream outs;
	private byte[] buffer;
	private static final int BUFFER_SIZE = 4096;
	private Thread thread;

	public BluetoothSlaveComm(BluetoothCtrl ctrl)
	{
		this.ctrl = ctrl;
		this.socket = ctrl.getSlave().getSocket();

		try
		{
			ins = socket.getInputStream();
			outs = socket.getOutputStream();
		}
		catch (IOException e){

		}

		buffer = new byte[BUFFER_SIZE];
		listen();
	}

	public void cleanUp()
	{
		if (thread != null)
			thread.interrupt();
	}

	private void listen()
	{
		thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				listen2();
			}
		});
		thread.start();
	}

	public interface CommandListener
	{
		void onPing();
		byte[] onFireShutter();
	}

	private CommandListener listener;

	public void setListener(CommandListener listener)
	{
		this.listener = listener;
	}

	private void listen2()
	{
		int read;
		while(true)
		{
			try
			{
				read = ins.read(buffer);
				ByteBuffer bb = ByteBuffer.wrap(buffer);

				int action = bb.getInt();

				if (action == BluetoothComm.PING)
				{
					doPing();
				}
				else if (action == BluetoothComm.FIRE_SHUTTER)
				{
					doShutter();
				}
			}
			catch(IOException e){
				break;
			}
		}
	}

	private void doPing()
	{
		if (listener != null)
			listener.onPing();

		try
		{
			byte[] bytes = ByteBuffer.allocate(4).putInt(BluetoothComm.PING).array();
			outs.write(bytes);
			outs.flush();
		}
		catch(IOException e){

		}
	}

	private void doShutter()
	{
		byte[] imgData = null;
		if (listener != null)
			imgData = listener.onFireShutter();

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(8);
			bb.putInt(BluetoothComm.FIRE_SHUTTER);

			if (imgData != null)
			{
				bb.putInt(imgData.length);
				outs.write(bb.array());
				outs.write(imgData);
			}
			else
			{
				bb.putInt(0);
				outs.write(bb.array());
			}

			outs.flush();
		}
		catch(IOException e){

		}
	}
}

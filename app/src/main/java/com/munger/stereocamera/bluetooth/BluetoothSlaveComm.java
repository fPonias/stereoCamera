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
		int onStatus();
		byte[] onFireShutter();
		void onLatencyCheck();
		void onFlip();
		float[] getViewAngles();
		void setZoom(float zoom);
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
				else if (action == BluetoothComm.GET_STATUS)
				{
					doGetStatus();
				}
				else if (action == BluetoothComm.FIRE_SHUTTER)
				{
					doShutter();
				}
				else if (action == BluetoothComm.LATENCY_CHECK)
				{
					doLatencyCheck();
				}
				else if (action == BluetoothComm.FLIP)
				{
					doFlip();
				}
				else if (action == BluetoothComm.GET_ANGLE_OF_VIEW)
				{
					doGetAngleOfView();
				}
				else if (action == BluetoothComm.SET_ZOOM)
				{
					float value = bb.getFloat();
					doSetZoom(value);
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

	private void doLatencyCheck()
	{
		long now = System.currentTimeMillis();
		if (listener != null)
			listener.onLatencyCheck();
		long diff = System.currentTimeMillis() - now;

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(12);
			bb.putInt(BluetoothComm.LATENCY_CHECK);
			bb.putLong(diff);
			outs.write(bb.array());
			outs.flush();
		}
		catch(IOException e){

		}
	}

	private void doFlip()
	{
		if (listener != null)
			listener.onFlip();

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(BluetoothComm.FLIP);
			outs.write(bb.array());
			outs.flush();
		}
		catch(IOException e){

		}
	}

	private void doGetAngleOfView()
	{
		float[] vals;
		if (listener != null)
			vals = listener.getViewAngles();
		else
			vals = new float[] { -1.0f, -1.0f};

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(12);
			bb.putInt(BluetoothComm.GET_ANGLE_OF_VIEW);
			bb.putFloat(vals[0]);
			bb.putFloat(vals[1]);
			outs.write(bb.array());
			outs.flush();
		}
		catch(IOException e){

		}
	}

	private void doSetZoom(float value)
	{
		if (listener != null)
			listener.setZoom(value);

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(BluetoothComm.SET_ZOOM);
			outs.write(bb.array());
			outs.flush();
		}
		catch(IOException e){

		}
	}

	private void doGetStatus()
	{
		int status = -1;
		if (listener != null)
			status = listener.onStatus();

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(8);
			bb.putInt(BluetoothComm.GET_STATUS);
			bb.putInt(status);
			outs.write(bb.array());
			outs.flush();
		}
		catch(IOException e){

		}
	}
}

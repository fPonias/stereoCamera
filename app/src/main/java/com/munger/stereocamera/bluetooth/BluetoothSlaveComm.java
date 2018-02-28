package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.munger.stereocamera.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
		CommandListener old = this.listener;
		this.listener = listener;

		if (old == null)
			listen();
	}

	private BluetoothCommands currentAction = BluetoothCommands.NONE;

	private void listen2()
	{
		int read;
		while(true)
		{
			try
			{
				int actionInt = getInt();
				currentAction = BluetoothCommands.values()[actionInt];
				Log.d("bluetoothSlaveComm", "command: " + currentAction.name() + " received");

				if (currentAction == BluetoothCommands.PING)
				{
					doPing();
				}
				else if (currentAction == BluetoothCommands.GET_STATUS)
				{
					doGetStatus();
				}
				else if (currentAction == BluetoothCommands.FIRE_SHUTTER)
				{
					doShutter();
				}
				else if (currentAction == BluetoothCommands.LATENCY_CHECK)
				{
					doLatencyCheck();
				}
				else if (currentAction == BluetoothCommands.FLIP)
				{
					doFlip();
				}
				else if (currentAction == BluetoothCommands.GET_ANGLE_OF_VIEW)
				{
					doGetAngleOfView();
				}
				else if (currentAction == BluetoothCommands.SET_ZOOM)
				{
					float value = getFloat();
					doSetZoom(value);
				}
			}
			catch(IOException e){
				break;
			}
		}
	}

	private int getInt() throws IOException
	{
		byte[] in = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(in);
		int read = ins.read(in);

		if (read < 4)
			throw new IOException ();

		return bb.getInt();
	}

	private float getFloat() throws IOException
	{
		byte[] in = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(in);
		int read = ins.read(in);

		if (read < 4)
			throw new IOException();

		return bb.getFloat();
	}

	private void doPing()
	{
		if (listener != null)
			listener.onPing();

		try
		{
			byte[] bytes = ByteBuffer.allocate(4).putInt(BluetoothCommands.PING.ordinal()).array();
			outs.write(bytes);
			outs.flush();
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
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
			bb.putInt(BluetoothCommands.FIRE_SHUTTER.ordinal());

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
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
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
			bb.putInt(BluetoothCommands.LATENCY_CHECK.ordinal());
			bb.putLong(diff);
			outs.write(bb.array());
			outs.flush();
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
		}
	}

	private void doFlip()
	{
		if (listener != null)
			listener.onFlip();

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(BluetoothCommands.FLIP.ordinal());
			outs.write(bb.array());
			outs.flush();
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
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
			bb.putInt(BluetoothCommands.GET_ANGLE_OF_VIEW.ordinal());
			bb.putFloat(vals[0]);
			bb.putFloat(vals[1]);
			outs.write(bb.array());
			outs.flush();
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
		}
	}

	private void doSetZoom(float value)
	{
		if (listener != null)
			listener.setZoom(value);

		try
		{
			ByteBuffer bb = ByteBuffer.allocate(4);
			bb.putInt(BluetoothCommands.SET_ZOOM.ordinal());
			outs.write(bb.array());
			outs.flush();
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
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
			bb.putInt(BluetoothCommands.GET_STATUS.ordinal());
			bb.putInt(status);
			byte[] array = bb.array();

			outs.write(array);
			outs.flush();
			Log.d("bluetoothSlaveComm", "success: " + currentAction.name());
		}
		catch(IOException e){
			Log.d("bluetoothSlaveComm", "failed: " + currentAction.name());
		}
	}
}

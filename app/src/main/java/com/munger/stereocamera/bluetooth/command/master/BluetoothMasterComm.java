package com.munger.stereocamera.bluetooth.command.master;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothMasterComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;
	private LinkedList<MasterCommand> tasks = new LinkedList<>();
	private final Object lock = new Object();
	private boolean cancelled = false;

	InputStream ins;
	OutputStream outs;
	byte[] buffer;
	static final int BUFFER_SIZE = 4096;

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

		Thread t = new Thread(new Runnable() {public void run()
		{
			while(!cancelled)
			{
				MasterCommand task = null;
				synchronized (lock)
				{
					if (tasks.isEmpty())
						try{lock.wait();}catch(InterruptedException e){break;}

					if (!tasks.isEmpty())
						task = tasks.removeFirst();
				}

				if (task != null)
				{
					task.setParent(BluetoothMasterComm.this);
					task.execute();
				}
			}
		}});
		t.start();
	}

	public void runCommand(MasterCommand command)
	{
		synchronized (lock)
		{
			tasks.add(command);
			lock.notify();
		}
	}

	void sendCommand(BluetoothCommands command) throws IOException
	{
		sendCommand(command, null);
	}

	void sendCommand(BluetoothCommands command, byte[] args) throws IOException
	{
		Log.d("BluetoothMasterComm", "command: " + command.name() + " started");
		outs.write((byte) command.ordinal());

		if (args != null)
			outs.write(args);

		outs.flush();

		int actionInt = ins.read();
		BluetoothCommands action = BluetoothCommands.values()[actionInt];

		if (action != command)
		{
			throw new IOException("incorrect response");
		}
	}

	public void cleanUp()
	{
		cancelled = true;
		synchronized (lock)
		{
			lock.notify();
		}
	}

	private byte[] longBuf = new byte[8];
	private ByteBuffer longBufBuf = ByteBuffer.wrap(longBuf);
	long readLong() throws IOException
	{
		int read;
		do
		{
			read = ins.read(longBuf, 0, 8);
		} while (read == 0);

		if (read < 8)
		{
			throw new IOException("incorrect response");
		}

		return longBufBuf.getLong(0);
	}

	private byte[] intBuf = new byte[4];
	private ByteBuffer intBufBuf = ByteBuffer.wrap(intBuf);
	int readInt() throws IOException
	{
		int read;
		do
		{
			read = ins.read(intBuf, 0, 4);
		} while (read == 0);

		if (read < 4)
		{
			throw new IOException("incorrect response");
		}

		return intBufBuf.getInt(0);
	}

	float readFloat() throws IOException
	{
		int read;
		do
		{
			read = ins.read(intBuf, 0, 4);
		} while (read == 0);

		if (read < 4)
		{
			throw new IOException("incorrect response");
		}

		return intBufBuf.getFloat(0);
	}
}

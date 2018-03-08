package com.munger.stereocamera.bluetooth.command.slave;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothSlaveComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;
	public InputStream ins;
	public OutputStream outs;

	private CommandProcessor commandProcessor;
	private InputProcessor inputProcessor;
	private OutputProcessor outputProcessor;

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

		outputProcessor = new OutputProcessor();
		outputProcessor.start();
		commandProcessor = new CommandProcessor(this);
		commandProcessor.start();
		inputProcessor = new InputProcessor(this);
		inputProcessor.start();
	}

	public void cleanUp()
	{
		inputProcessor.cleanUp();
		commandProcessor.cleanUp();
		outputProcessor.cleanUp();
	}

	public interface Listener
	{
		void onCommand(SlaveCommand command);
	}

	public void addListener(BluetoothCommands command, Listener listener)
	{
		commandProcessor.addListener(command, listener);
	}

	public void processCommand(SlaveCommand command)
	{
		command.setComm(this);
		commandProcessor.processCommand(command);
	}

	private boolean pingReceived = false;
	private boolean isSending = false;
	private ConcurrentLinkedQueue<SlaveCommand> commandBacklog = new ConcurrentLinkedQueue<>();
	private final Object lock = new Object();

	public void sendCommand(SlaveCommand command)
	{
		synchronized (lock)
		{
			if (pingReceived)
			{
				commandBacklog.add(command);
			}
			else
			{
				if (command.command != BluetoothCommands.PING)
				{
					commandBacklog.add(command);
					return;
				}
				else
				{
					command.setComm(this);
					outputProcessor.sendCommand(command);
					pingReceived = true;
				}
			}

			if (isSending)
				return;

			isSending = true;
		}

		while (!commandBacklog.isEmpty())
		{
			try
			{
				SlaveCommand cmd = commandBacklog.remove();
				cmd.setComm(this);
				outputProcessor.sendCommand(cmd);
			}
			catch(NoSuchElementException e){}

		}

		synchronized (lock)
		{
			isSending = false;
		}
	}


	private byte[] intbuf = new byte[4];
	ByteBuffer intbb = ByteBuffer.wrap(intbuf);
	public int getInt() throws IOException
	{
		int read = ins.read(intbuf);

		if (read < 4)
			throw new IOException ();

		return intbb.getInt(0);
	}

	public float getFloat() throws IOException
	{
		int read = ins.read(intbuf);

		if (read < 4)
			throw new IOException();

		return intbb.getFloat(0);
	}

	ByteBuffer ointbb = ByteBuffer.allocate(4);

	public void putInt(int val) throws IOException
	{
		//Log.d("bluetooth slave", "writing int " + val);
		ointbb.putInt(0, val);
		outs.write(ointbb.array());
		outs.flush();
	}

	public void putFloat(float val) throws IOException
	{
		//Log.d("bluetooth slave", "writing float " + val);
		ointbb.putFloat(0, val);
		outs.write(ointbb.array());
		outs.flush();
	}

	ByteBuffer olongbb = ByteBuffer.allocate(8);
	public void putLong(long val) throws IOException
	{
		//Log.d("bluetooth slave", "writing long " + val);
		olongbb.putLong(0, val);
		outs.write(olongbb.array());
		outs.flush();
	}
}

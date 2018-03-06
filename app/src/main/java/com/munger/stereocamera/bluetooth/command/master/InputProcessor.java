package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;
import com.munger.stereocamera.bluetooth.command.master.listeners.MasterListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class InputProcessor
{
	private BluetoothMasterComm parent;
	private boolean cancelled = false;
	private final Object lock = new Object();
	private HashMap<BluetoothCommands, MasterListener> slaveListeners = new HashMap<>();
	private Thread slaveProcessor;

	public InputProcessor(BluetoothMasterComm parent)
	{
		this.parent = parent;
	}

	public void cleanUp()
	{
		synchronized (lock)
		{
			if (cancelled)
				return;

			cancelled = true;
			lock.notify();
		}
	}

	private byte[] readBuf = new byte[5];
	private ByteBuffer readBuffer = ByteBuffer.wrap(readBuf);

	private class commandStruct
	{
		public BluetoothCommands command;
		public int id;
	}

	public void registerListener(MasterListener listener)
	{
		listener.setParent(parent);
		slaveListeners.put(listener.getCommand(), listener);
	}

	public void start()
	{
		slaveProcessor = new Thread(new Runnable() { public void run() { runSlaveProcessor();} });
		slaveProcessor.start();
	}

	private void runSlaveProcessor()
	{
		pendingCommands = new HashMap<>();

		while(!cancelled)
		{
			commandStruct str = readCommand();

			synchronized (lock)
			{
				if (cancelled || str == null)
					return;
			}

			if (str.id != -1)
				handleResponse(str);
			else
				handleSlaveCommand(str);
		}
	}

	private commandStruct readCommand()
	{
		try
		{
			int read = parent.ins.read(readBuf);

			if (read == 0)
				return null;

			commandStruct ret = new commandStruct();
			//Log.d("masterReader", "read command: " + readBuf[0]);
			ret.command = BluetoothCommands.values()[readBuf[0]];
			//Log.d("masterReader", "read command: " + ret.command.name());
			ret.id = readBuffer.getInt(1);
			//Log.d("masterReader", "read command id: " + ret.id);

			return ret;
		}
		catch(IOException e){
			//handle system failure
			return null;
		}
	}

	HashMap<Integer, MasterCommand> pendingCommands;

	private void handleResponse(commandStruct str)
	{
		if (!pendingCommands.containsKey(str.id))
		{
			//handle command failure
			return;
		}

		MasterCommand ret = pendingCommands.remove(str.id);

		if (ret.getCommand() != str.command)
		{
			//handle mismatched response failure
			return;
		}

		try
		{
			ret.handleResponse();
		}
		catch (IOException e){
			ret.onExecuteFail();
			return;
		}
	}

	private void handleSlaveCommand(commandStruct str)
	{
		if (!slaveListeners.containsKey(str.command))
		{
			//handle missing handler failure
			return;
		}

		MasterListener listener = slaveListeners.get(str.command);

		try
		{
			listener.handleResponse();
		}
		catch(IOException e){
			listener.onFail();
			return;
		}
	}
}

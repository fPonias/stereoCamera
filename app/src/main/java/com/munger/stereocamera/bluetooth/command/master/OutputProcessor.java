package com.munger.stereocamera.bluetooth.command.master;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class OutputProcessor
{
	private Thread commandProcessor;
	private LinkedList<BluetoothMasterComm.CommandArg> tasks = new LinkedList<>();
	private BluetoothMasterComm parent;

	private final Object lock = new Object();
	private boolean cancelled = false;

	public OutputProcessor(BluetoothMasterComm comm)
	{
		this.parent = comm;
	}

	public void start()
	{

		commandProcessor = new Thread(new Runnable() {public void run()
		{
			runCommandProcessor();
		}});
		commandProcessor.start();
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

	private void runCommandProcessor()
	{
		int id = 1;

		while(!cancelled)
		{
			BluetoothMasterComm.CommandArg task = null;
			synchronized (lock)
			{
				if (tasks.isEmpty())
					try{lock.wait();}catch(InterruptedException e){break;}

				if (cancelled)
					return;

				if (!tasks.isEmpty())
					task = tasks.removeFirst();
			}

			if (task != null)
			{
				task.command.setParent(parent);
				task.command.setId(id);
				parent.inputProcessor.pendingCommands.put(id, task);
				id++;
				task.command.onStart();

				try
				{
					sendCommand(task);
				}
				catch(IOException e){
					cleanUp();
					break;
				}
			}
		}
	}

	private ByteBuffer sendBuffer = ByteBuffer.allocate(5);

	private void sendCommand(BluetoothMasterComm.CommandArg arg) throws IOException
	{
		BluetoothCommands comm = arg.command.getCommand();
		Log.d("BluetoothMasterComm", "command: " + comm.name() + " started");
		sendBuffer.put(0, (byte) comm.ordinal());
		sendBuffer.putInt(1, arg.command.getId());
		parent.writeBytes(sendBuffer.array());

		byte[] args = arg.command.getArguments();
		if (args != null)
			parent.writeBytes(args);
	}

	public void runCommand(BluetoothMasterComm.CommandArg command)
	{
		synchronized (lock)
		{
			tasks.add(command);
			lock.notify();
		}
	}
}

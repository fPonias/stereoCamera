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
	private LinkedList<MasterCommand> tasks = new LinkedList<>();
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
			MasterCommand task = null;
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
				task.setParent(parent);
				task.setId(id);
				parent.inputProcessor.pendingCommands.put(id, task);
				id++;
				task.onStart();
				sendCommand(task);
			}
		}
	}

	private ByteBuffer sendBuffer = ByteBuffer.allocate(5);

	private void sendCommand(MasterCommand command)
	{
		try
		{
			BluetoothCommands comm = command.getCommand();
			Log.d("BluetoothMasterComm", "command: " + comm.name() + " started");
			sendBuffer.put(0, (byte) comm.ordinal());
			sendBuffer.putInt(1, command.getId());
			parent.outs.write(sendBuffer.array());

			byte[] args = command.getArguments();
			if (args != null)
				parent.outs.write(args);

			parent.outs.flush();
		}
		catch(IOException e){
			command.onExecuteFail();
		}
	}

	public void runCommand(MasterCommand command)
	{
		synchronized (lock)
		{
			tasks.add(command);
			lock.notify();
		}
	}
}

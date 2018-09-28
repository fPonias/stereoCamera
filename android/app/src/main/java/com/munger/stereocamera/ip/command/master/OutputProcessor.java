package com.munger.stereocamera.ip.command.master;

import android.util.Log;

import com.munger.stereocamera.ip.command.BluetoothCommands;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class OutputProcessor
{
	private Thread commandProcessor;
	private LinkedList<MasterComm.CommandArg> tasks = new LinkedList<>();
	private MasterComm parent;

	private final Object lock = new Object();
	private boolean cancelled = false;

	public OutputProcessor(MasterComm comm)
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
			MasterComm.CommandArg task = null;
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

	private void sendCommand(MasterComm.CommandArg arg) throws IOException
	{
		BluetoothCommands comm = arg.command.getCommand();
		Log.d("MasterComm", "command: " + comm.name() + " started");
		sendBuffer.put(0, (byte) comm.ordinal());
		sendBuffer.putInt(1, arg.command.getId());
		parent.writeBytes(sendBuffer.array());

		byte[] args = arg.command.getArguments();
		if (args != null)
			parent.writeBytes(args);

		int sz = arg.command.getFileSz();

		if (sz > 0)
		{
			String path = arg.command.getFilePath();
			FileInputStream fis = new FileInputStream(path);
			parent.writeStream(sz, fis);
			fis.close();
		}
	}

	public void runCommand(MasterComm.CommandArg command)
	{
		synchronized (lock)
		{
			tasks.add(command);
			lock.notify();
		}
	}
}

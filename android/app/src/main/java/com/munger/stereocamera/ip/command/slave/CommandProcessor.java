package com.munger.stereocamera.ip.command.slave;

import android.util.Log;

import com.munger.stereocamera.ip.command.Command;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class CommandProcessor
{
	private boolean isCancelled = false;

	private final Object commandLock = new Object();
	private LinkedList<SlaveCommand> commands = new LinkedList<>();
	private SlaveComm parent;

	public CommandProcessor(SlaveComm parent)
	{
		this.parent = parent;
	}

	public void cleanUp()
	{
		synchronized (commandLock)
		{
			if (isCancelled)
				return;

			isCancelled = true;
			commandLock.notify();
		}
	}

	public void start()
	{
		Thread consumer = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (!isCancelled)
				{
					commandProcessor();
				}
			}
		});
		consumer.start();
	}

	private HashMap<Command.Type, SlaveComm.Listener> commandListeners = new HashMap<>();

	public void addListener(Command.Type command, SlaveComm.Listener listener)
	{
		commandListeners.put(command, listener);
	}

	private void commandProcessor()
	{
		SlaveCommand command;
		synchronized (commandLock)
		{
			if (commands.isEmpty())
			{
				try {commandLock.wait();}
				catch(InterruptedException e){return;}
			}

			if (isCancelled)
				return;

			command = commands.removeFirst();
		}

		if (commandListeners.containsKey(command.command))
		{
			commandListeners.get(command.command).onCommand(command);
		}
		else
		{
			Log.d("CommandProcessor", "no handler for command " + command.command.name());
		}

		parent.sendCommand(command);
	}

	public void processCommand(SlaveCommand command)
	{
		synchronized (commandLock)
		{
			commands.add(command);
			commandLock.notify();
		}
	}
}

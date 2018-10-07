package com.munger.stereocamera.ip.command.master;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.commands.MasterCommand;
import com.munger.stereocamera.ip.command.master.commands.MasterCommandFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class InputProcessor
{
	private MasterComm parent;
	private boolean cancelled = false;
	private final Object lock = new Object();
	private HashMap<Command.Type, MasterComm.SlaveListener> slaveListeners = new HashMap<>();
	private Thread slaveProcessor;

	public InputProcessor(MasterComm parent)
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
		public Command.Type command;
		public int id;
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
			commandStruct str;
			try
			{
				str = readCommand();
			}
			catch(IOException e){
				cleanUp();
				break;
			}

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

	private commandStruct readCommand() throws IOException
	{
		parent.waitForData();

		commandStruct ret = new commandStruct();

		byte actionInt = parent.readByte();

		Command.Type[] cmds = Command.Type.values();
		if (actionInt < 0 || actionInt >= cmds.length)
			throw new IOException("Unknown Type command " + actionInt);

		ret.command = cmds[actionInt];
		ret.id = parent.readInt();

		return ret;
	}

	HashMap<Integer, MasterComm.CommandArg> pendingCommands;

	private void handleResponse(commandStruct str)
	{
		MasterCommand command;
		MasterComm.SlaveListener listener;

		if (!pendingCommands.containsKey(str.id))
		{
			command = MasterCommandFactory.get(str.command, str.id);
			listener = null;
		}
		else
		{
			MasterComm.CommandArg arg = pendingCommands.remove(str.id);
			command = arg.command;
			listener = arg.listener;
		}

		MasterIncoming response = null;

		try
		{
			response = command.getResponse();
			response.setParent(parent);
			response.readResponse();
		}
		catch (IOException e){
		}

		if (listener != null)
			listener.onResponse(response);
	}

	public void registerListener(Command.Type command, MasterComm.SlaveListener listener)
	{
		slaveListeners.put(command, listener);
	}

	private void handleSlaveCommand(commandStruct str)
	{
		MasterIncoming response = null;

		try
		{
			response = InputProcessorFactory.getCommand(str.command);
			response.setParent(parent);
			response.readResponse();
		}
		catch(IOException e){

		}

		if (slaveListeners.containsKey(str.command))
		{
			MasterComm.SlaveListener listener = slaveListeners.get(str.command);
			listener.onResponse(response);
		}
	}
}

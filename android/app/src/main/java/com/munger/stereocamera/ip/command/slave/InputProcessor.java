package com.munger.stereocamera.ip.command.slave;

import android.util.Log;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.commands.Handshake;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveConnectionPause;
import com.munger.stereocamera.ip.command.slave.commands.GetLatency;
import com.munger.stereocamera.ip.command.slave.commands.Ping;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveDisconnect;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveFacing;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveOverlay;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveProcessedPhoto;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveVersion;
import com.munger.stereocamera.ip.command.slave.commands.ReceiveZoom;
import com.munger.stereocamera.ip.command.slave.commands.Shutter;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class InputProcessor
{
	private boolean isCancelled = false;
	private final Object commandLock = new Object();
	private SlaveComm parent;

	public InputProcessor(SlaveComm parent)
	{
		this.parent = parent;
	}

	public void cleanUp()
	{
		synchronized (commandLock)
		{
			if(isCancelled)
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
					try
					{
						commandListener();
					}
					catch(IOException e){
						break;
					}

				}
			}
		});
		consumer.start();
	}

	private void commandListener() throws IOException
	{
		byte actionInt = parent.getByte();

		Command.Type[] cmds = Command.Type.values();
		if (actionInt < 0 || actionInt >= cmds.length)
			throw new IOException("Unknown Type command " + actionInt);

		Command.Type currentAction = cmds[actionInt];
		int id = parent.getInt();

		if (isCancelled)
			return;

		SlaveCommand command;

		switch(currentAction)
		{
			case PING:
				command = new Ping(id);
				break;
			case HANDSHAKE:
				command = new Handshake(id);
				break;
			case SET_ZOOM:
				command = new ReceiveZoom(id);
				break;
			case SET_FACING:
				command = new ReceiveFacing(id);
				break;
			case SEND_VERSION:
				command = new ReceiveVersion(id);
				break;
			case FIRE_SHUTTER:
				command = new Shutter(id);
				break;
			case LATENCY_CHECK:
				command = new GetLatency(id);
				break;
			case SET_OVERLAY:
				command = new ReceiveOverlay(id);
				break;
			case SEND_PROCESSED_PHOTO:
				command = new ReceiveProcessedPhoto(id);
				break;
			case CONNECTION_PAUSE:
				command = new ReceiveConnectionPause(id);
				break;
			case DISCONNECT:
				command = new ReceiveDisconnect(id);
				break;
			default:
				Log.d("bluetoothSlaveComm", "unknown command " + currentAction.name());
				throw new IOException("unknown command " + currentAction.name());
		}

		command.setComm(parent);
		command.readArguments();

		parent.processCommand(command);

		Log.d("bluetoothSlaveComm", "command: " + currentAction.name() + " received");
	}
}

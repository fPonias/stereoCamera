package com.munger.stereocamera.bluetooth.command.slave;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.commands.GetLatency;
import com.munger.stereocamera.bluetooth.command.slave.commands.Ping;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveFacing;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveOverlay;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveZoom;
import com.munger.stereocamera.bluetooth.command.slave.commands.Shutter;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class InputProcessor
{
	private boolean isCancelled = false;
	private final Object commandLock = new Object();
	private BluetoothSlaveComm parent;

	public InputProcessor(BluetoothSlaveComm parent)
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
		int actionInt = parent.ins.read();
		BluetoothCommands currentAction = BluetoothCommands.values()[actionInt];
		int id = parent.getInt();

		if (isCancelled)
			return;

		SlaveCommand command;

		switch(currentAction)
		{
			case PING:
				command = new Ping(id);
				break;
			case SET_ZOOM:
				command = new ReceiveZoom(id);
				break;
			case SET_FACING:
				command = new ReceiveFacing(id);
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

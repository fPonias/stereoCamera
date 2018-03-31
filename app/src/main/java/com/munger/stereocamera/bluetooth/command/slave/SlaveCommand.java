package com.munger.stereocamera.bluetooth.command.slave;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class SlaveCommand
{
	public BluetoothCommands command;
	public int id = -1;

	protected BluetoothSlaveComm comm;

	public SlaveCommand()
	{
	}

	public void setComm(BluetoothSlaveComm comm)
	{
		this.comm = comm;
	}

	public interface Listener
	{
		void onStarted();

		void onFinished();
	}

	private Listener listener;

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}

	public void doSend() throws IOException
	{
		if (listener != null)
			listener.onStarted();

		//Log.d("bluetooth slave", "writing command " + command.name());
		comm.putByte((byte) command.ordinal());
		comm.putInt(id);

		send();

		if (listener != null)
			listener.onFinished();
	}

	protected void send() throws IOException
	{}

	protected void readArguments() throws IOException
	{
	}
}

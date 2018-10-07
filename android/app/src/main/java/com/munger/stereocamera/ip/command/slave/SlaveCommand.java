package com.munger.stereocamera.ip.command.slave;

import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SlaveCommand
{
	public Command.Type command;
	public int id = -1;

	protected SlaveComm comm;

	public SlaveCommand()
	{
	}

	public void setComm(SlaveComm comm)
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

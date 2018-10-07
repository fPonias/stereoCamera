package com.munger.stereocamera.ip.command.commands;

import android.util.Log;

import com.munger.stereocamera.ip.command.Command;

public class Ping extends Command
{
	private long start;
	public long value;

	public Ping()
	{
		super();

		cmdtype = Type.PING;
		expectsResponse = true;

		start = System.currentTimeMillis();
		value = -1;
	}

	@Override
	public void onResponse(Command command)
	{
		long end = System.currentTimeMillis();
		value = end - start;

		Log.d("Command", "ping took " + value + " millis");
	}
}

package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/5/18.
 */

public class GetLatency extends SlaveCommand
{
	public GetLatency(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.LATENCY_CHECK;
	}

	private long latency;

	public void setLatency(long latency)
	{
		this.latency = latency;
	}

	@Override
	public void send() throws IOException
	{
		comm.putLong(latency);
	}
}

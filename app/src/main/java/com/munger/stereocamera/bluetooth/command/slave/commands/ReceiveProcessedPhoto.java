package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/20/18.
 */

public class ReceiveProcessedPhoto extends SlaveCommand
{
	private int sz;
	private byte[] data;

	public byte[] getData()
	{
		return data;
	}

	public ReceiveProcessedPhoto(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.SEND_PROCESSED_PHOTO;
	}

	@Override
	protected void readArguments() throws IOException
	{
		sz = comm.getInt();
		data = new byte[sz];

		int total = 0;
		int read = 1;
		while (read > 0 && total < sz)
		{
			read = comm.ins.read(data, total, sz - total);
			total += read;
		}
	}
}

package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.IOException;

public class Shutter extends MasterCommand
{
	public Shutter()
	{

	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.FIRE_SHUTTER;
	}

	@Override
	public MasterIncoming getResponse()
	{
		return new Response(id);
	}

	public class Response extends MasterIncoming
	{
		public float zoom;
		public PhotoOrientation orientation;
		public byte[] data;

		public Response(int id)
		{
			super(BluetoothCommands.FIRE_SHUTTER, id);
		}

		@Override
		public void readResponse() throws IOException
		{
			orientation = PhotoOrientation.values()[parent.readInt()];
			zoom = parent.readFloat();

			int size = parent.readInt();
			data = parent.readBytes(size);
		}
	}
}

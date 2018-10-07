package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Shutter extends MasterCommand
{
	private SHUTTER_TYPE type;

	public Shutter(SHUTTER_TYPE type)
	{
		this.type = type;
	}

	@Override
	public Command.Type getCommand()
	{
		return Command.Type.FIRE_SHUTTER;
	}


	@Override
	public byte[] getArguments()
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(type.ordinal());
		return bb.array();
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
			super(Command.Type.FIRE_SHUTTER, id);
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

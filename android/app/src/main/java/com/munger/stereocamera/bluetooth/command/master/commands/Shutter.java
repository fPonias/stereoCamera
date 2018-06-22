package com.munger.stereocamera.bluetooth.command.master.commands;

import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

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
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.FIRE_SHUTTER;
	}

	public enum SHUTTER_TYPE
	{
		PREVIEW,
		LO_RES,
		HI_RES
	};

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

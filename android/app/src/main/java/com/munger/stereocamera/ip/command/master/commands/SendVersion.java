package com.munger.stereocamera.ip.command.master.commands;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.munger.stereocamera.BuildConfig;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 5/14/18.
 */

public class SendVersion extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.SEND_VERSION;
	}

	public int getVersion()
	{
		return BuildConfig.VERSION_CODE;
	}

	@Override
	public byte[] getArguments()
	{
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putInt(getVersion());
		return buf.array();
	}

	@Override
	public MasterIncoming getResponse()
	{
		return new Response(id);
	}

	public class Response extends MasterIncoming
	{
		public int version;

		public Response(int id)
		{
			super(BluetoothCommands.SEND_VERSION, id);
		}

		@Override
		public void readResponse() throws IOException
		{
			version = parent.readInt();
		}
	}
}

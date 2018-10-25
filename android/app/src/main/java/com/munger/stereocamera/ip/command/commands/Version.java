package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.BuildConfig;
import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class Version extends Command
{
	public int version = -1;
	public Platform platform = Platform.ANDROID;

	public enum Platform
	{
		NONE,
		IOS,
		ANDROID
	}

	public Version()
	{
		super();
		cmdtype = Type.SEND_VERSION;
		expectsResponse = true;

		version = getVersion();
	}

	public static int getVersion()
	{
		return BuildConfig.VERSION_CODE;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);

		if (!result)
			{ return false; }

		try
		{
			comm.putByte((byte) platform.ordinal());
			comm.putInt(version);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}

	@Override
	public boolean receive(Comm comm)
	{
		boolean result = super.receive(comm);

		if (!result)
			{ return false; }

		try
		{
			byte idx = comm.getByte();
			if (idx < 0 || idx > 2)
				return false;

			platform = Platform.values()[idx];

			version = comm.getInt();
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

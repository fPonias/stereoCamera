package com.munger.stereocamera.ip.command.slave.commands;

import android.content.pm.PackageInfo;
import android.os.Build;

import com.munger.stereocamera.BuildConfig;
import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.IOException;

/**
 * Created by hallmarklabs on 5/14/18.
 */

public class ReceiveVersion extends SlaveCommand
{
	public ReceiveVersion(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.SEND_VERSION;
	}

	private int version;

	public int getVersion()
	{
		return version;
	}

	@Override
	protected void readArguments() throws IOException
	{
		version = comm.getInt();
	}

	@Override
	public void send() throws IOException
	{
		comm.putInt(BuildConfig.VERSION_CODE);
	}
}

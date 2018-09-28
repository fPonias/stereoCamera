package com.munger.stereocamera.ip.command.slave.commands;

import android.util.Log;

import com.munger.stereocamera.ip.command.BluetoothCommands;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReceiveProcessedPhoto extends SlaveCommand
{
	private int sz;
	private String tmpPath;

	public String getTmpPath()
	{
		return tmpPath;
	}

	public void cleanUp()
	{
		File tmp = new File(tmpPath);
		tmp.delete();
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
		File tmp = File.createTempFile("pto", null);
		Log.d("ReceiveProcessedPhoto", "created tmp file at " + tmp.getPath());

		FileOutputStream fos = new FileOutputStream(tmp);
		comm.pipeData(sz, fos);
		fos.flush();
		fos.close();
		Log.d("ReceiveProcessPhoto", "wrote " + sz + " bytes to tmp");

		tmpPath = tmp.getPath();
	}
}

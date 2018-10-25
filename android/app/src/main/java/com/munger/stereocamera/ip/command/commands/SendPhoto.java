package com.munger.stereocamera.ip.command.commands;

import android.util.Log;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SendPhoto extends Command
{
	public byte[] data;
	public File file;

	public SendPhoto()
	{
		super();
		doInit();
	}

	public SendPhoto(String path)
	{
		super();

		file = new File(path);
		doInit();
	}

	public SendPhoto(byte[] data)
	{
		super();
		this.data = data;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.SEND_PROCESSED_PHOTO;
		expectsResponse = true;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false; }

		try
		{
			if (data != null)
			{
				comm.putLong(data.length);

				if (data.length > 0)
					comm.putData(data);
			}
			else if (file != null)
			{
				long sz = file.length();
				comm.putLong(sz);

				if (sz > 0)
					comm.putFile(file);
			}
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
			long sz = comm.getLong();

			if (sz == 0)
			{
				data = new byte[0];
				return true;
			}

			File tmp = File.createTempFile("pto", null);
			Log.d("stereoCamera", "created tmp file at " + tmp.getPath());

			FileOutputStream fos = new FileOutputStream(tmp);
			comm.pipeData((int) sz, fos);
			fos.flush();
			fos.close();
			Log.d("stereoCamera", "wrote " + sz + " bytes to tmp");

			tmpPath = tmp.getPath();
			data = comm.getData((int) sz);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}

	private String tmpPath;

	public String getTmpPath()
	{
		return tmpPath;
	}
}

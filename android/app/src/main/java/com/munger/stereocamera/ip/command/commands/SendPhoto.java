package com.munger.stereocamera.ip.command.commands;

import android.net.Uri;
import android.util.Log;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SendPhoto extends Command
{
	public long id;
	public File file;

	public SendPhoto()
	{
		super();
		doInit();
	}

	public SendPhoto(long id)
	{
		super();

		this.id = id;
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
			PhotoFiles files = PhotoFiles.Factory.get();
			long sz = files.getSize(id);
			if (sz <= 0)
			{
				comm.putLong(sz);
				return true;
			}

			InputStream ins = files.getStream(id);
			comm.putLong(sz);
			comm.putStream(ins, sz);
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

			if (sz <= 0)
			{
				file = null;
				return true;
			}

			File tmp = File.createTempFile("pto", null);
			Log.d("stereoCamera", "created tmp file at " + tmp.getPath());

			FileOutputStream fos = new FileOutputStream(tmp);
			comm.pipeData((int) sz, fos);
			fos.flush();
			fos.close();
			Log.d("stereoCamera", "wrote " + sz + " bytes to tmp");

			String tmpPath = tmp.getPath();
			file = new File(tmpPath);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

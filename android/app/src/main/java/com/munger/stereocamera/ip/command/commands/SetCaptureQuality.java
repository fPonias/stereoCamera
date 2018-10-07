package com.munger.stereocamera.ip.command.commands;

import android.media.Image;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;

import java.io.IOException;

public class SetCaptureQuality extends Command
{
	public enum ImageQuality
	{
		PREVIEW,
		LO_RES,
		HI_RES
	};

	public ImageQuality quality;

	public SetCaptureQuality()
	{
		super();
		doInit();
	}

	public SetCaptureQuality(ImageQuality quality)
	{
		super();
		this.quality = quality;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.SET_CAPTURE_QUALITY;
		expectsResponse = true;
	}

	@Override
	public boolean receive(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false; }

		try
		{
			byte b = comm.getByte();
			ImageQuality[] quals = ImageQuality.values();

			if (b < 0 || b >= quals.length)
				{ return false; }

			quality = quals[b];
		}
		catch(IOException e ) {
			return false;
		}

		return true;
	}

	@Override
	public boolean send(Comm comm)
	{
		boolean result = super.send(comm);
		if (!result)
			{ return false; }

		try
		{
			comm.putByte((byte) quality.ordinal());
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

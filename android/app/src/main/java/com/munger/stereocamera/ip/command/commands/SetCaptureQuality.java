package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.widget.PreviewWidget;

import java.io.IOException;

public class SetCaptureQuality extends Command
{
	public PreviewWidget.ShutterType quality;

	public SetCaptureQuality()
	{
		super();
		doInit();
	}

	public SetCaptureQuality(PreviewWidget.ShutterType quality)
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
		boolean result = super.receive(comm);
		if (!result)
			{ return false; }

		try
		{
			byte b = comm.getByte();
			PreviewWidget.ShutterType[] quals = PreviewWidget.ShutterType.values();

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
			byte val = (byte) quality.ordinal();
			comm.putByte(val);
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

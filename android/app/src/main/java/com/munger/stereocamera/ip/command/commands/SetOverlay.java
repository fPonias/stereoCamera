package com.munger.stereocamera.ip.command.commands;

import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.widget.PreviewOverlayWidget;

import java.io.IOException;

public class SetOverlay extends Command
{
	public PreviewOverlayWidget.Type type;

	public SetOverlay()
	{
		super();
		doInit();
	}

	public SetOverlay(PreviewOverlayWidget.Type type)
	{
		super();
		this.type = type;
		doInit();
	}

	private void doInit()
	{
		cmdtype = Type.SET_OVERLAY;
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
			comm.putByte((byte) type.ordinal());
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
			byte b = comm.getByte();

			PreviewOverlayWidget.Type[] types = PreviewOverlayWidget.Type.values();
			if (b < 0 || b >= types.length)
				return false;

			type = types[b];
		}
		catch(IOException e){
			return false;
		}

		return true;
	}
}

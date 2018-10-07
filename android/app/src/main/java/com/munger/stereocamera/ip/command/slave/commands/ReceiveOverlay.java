package com.munger.stereocamera.ip.command.slave.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.slave.SlaveCommand;
import com.munger.stereocamera.widget.PreviewOverlayWidget;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/19/18.
 */

public class ReceiveOverlay extends SlaveCommand
{
	private PreviewOverlayWidget.Type type;

	public PreviewOverlayWidget.Type getType()
	{
		return type;
	}

	public ReceiveOverlay(int i)
	{
		super();
		this.id = i;
		this.command = Command.Type.SET_OVERLAY;
	}

	@Override
	protected void readArguments() throws IOException
	{
		int arg = comm.getInt();
		type = PreviewOverlayWidget.Type.values()[arg];
	}

}

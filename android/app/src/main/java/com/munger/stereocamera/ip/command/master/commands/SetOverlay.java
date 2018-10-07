package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.widget.PreviewOverlayWidget;

import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 3/19/18.
 */

public class SetOverlay extends MasterCommand
{
	private PreviewOverlayWidget.Type type;
	public SetOverlay(PreviewOverlayWidget.Type type)
	{
		super();
		this.type = type;
	}

	@Override
	public Command.Type getCommand()
	{
		return Command.Type.SET_OVERLAY;
	}

	@Override
	public byte[] getArguments()
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(type.ordinal());
		return bb.array();
	}
}

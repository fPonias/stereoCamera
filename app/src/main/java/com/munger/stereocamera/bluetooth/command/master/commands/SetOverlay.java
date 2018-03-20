package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.widget.PreviewOverlayWidget;

import java.io.IOException;
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
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.SET_OVERLAY;
	}

	@Override
	public void onExecuteFail()
	{}

	@Override
	public void handleResponse() throws IOException
	{}

	@Override
	public byte[] getArguments()
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(type.ordinal());
		return bb.array();
	}
}

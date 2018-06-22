package com.munger.stereocamera.bluetooth.command.master.listeners;

import android.util.Log;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.IOException;

/**
 * Created by hallmarklabs on 3/8/18.
 */

public class ReceiveOrientation extends MasterIncoming
{
	public PhotoOrientation orientation;

	public ReceiveOrientation()
	{
		super(BluetoothCommands.RECEIVE_ORIENTATION, -1);
	}

	public void readResponse() throws IOException
	{
		byte ret = parent.readByte();
		orientation = PhotoOrientation.values()[ret];
	}
}

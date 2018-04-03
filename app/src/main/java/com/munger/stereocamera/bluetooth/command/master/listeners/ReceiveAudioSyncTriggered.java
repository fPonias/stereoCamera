package com.munger.stereocamera.bluetooth.command.master.listeners;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.IOException;

public class ReceiveAudioSyncTriggered extends MasterIncoming
{
	public ReceiveAudioSyncTriggered()
	{
		super(BluetoothCommands.RECEIVE_AUDIO_SYNC_TRIGGERED, -1);
	}

	public int triggerCmdId;

	@Override
	public void readResponse() throws IOException
	{
		triggerCmdId = parent.readInt();
	}
}

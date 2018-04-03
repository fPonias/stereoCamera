package com.munger.stereocamera.bluetooth.command.slave.senders;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

import java.io.IOException;

public class SendAudioSyncTriggered extends SlaveCommand
{
	public SendAudioSyncTriggered(int cmdId)
	{
		super();
		this.id = -1;
		this.command = BluetoothCommands.RECEIVE_AUDIO_SYNC_TRIGGERED;
		this.triggerCmdId = cmdId;
	}

	public int triggerCmdId;

	@Override
	protected void send() throws IOException
	{
		comm.putInt(triggerCmdId);
	}
}

package com.munger.stereocamera.bluetooth.command.slave.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;

public class AudioSyncTriggered extends SlaveCommand
{
	public AudioSyncTriggered(int id)
	{
		super();
		this.id = id;
		this.command = BluetoothCommands.AUDIO_SYNC_TRIGGERED;
	}
}

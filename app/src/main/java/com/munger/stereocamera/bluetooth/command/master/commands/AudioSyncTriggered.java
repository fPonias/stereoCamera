package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;


public class AudioSyncTriggered extends MasterCommand
{
	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.AUDIO_SYNC_TRIGGERED;
	}
}

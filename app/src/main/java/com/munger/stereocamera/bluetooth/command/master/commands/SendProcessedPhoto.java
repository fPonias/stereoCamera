package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SendProcessedPhoto extends MasterCommand
{
	private String path;
	private File file;

	public SendProcessedPhoto(String path)
	{
		this.path = path;
		file = new File(path);
	}

	@Override
	public BluetoothCommands getCommand()
	{
		return BluetoothCommands.SEND_PROCESSED_PHOTO;
	}

	@Override
	public void onExecuteFail()
	{}

	@Override
	public int getFileSz()
	{
		return (int) file.length();
	}

	@Override
	public String getFilePath()
	{
		return path;
	}
}

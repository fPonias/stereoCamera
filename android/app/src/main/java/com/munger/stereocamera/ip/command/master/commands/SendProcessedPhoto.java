package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

import java.io.File;

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
	public Command.Type getCommand()
	{
		return Command.Type.SEND_PROCESSED_PHOTO;
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

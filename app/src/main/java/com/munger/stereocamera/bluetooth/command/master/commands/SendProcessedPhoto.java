package com.munger.stereocamera.bluetooth.command.master.commands;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by hallmarklabs on 3/20/18.
 */

public class SendProcessedPhoto extends MasterCommand
{
	private String path;

	public SendProcessedPhoto(String path)
	{
		this.path = path;
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
	public void handleResponse() throws IOException
	{}

	@Override
	public byte[] getArguments()
	{
		RandomAccessFile raf = null;
		FileChannel ch = null;
		ByteBuffer bb = null;
		byte[] ret = null;
		try
		{
			raf = new RandomAccessFile(path, "r");
			ch = raf.getChannel();

			int sz = (int) ch.size();
			bb = ByteBuffer.allocate(4 + sz);
			bb.putInt(sz);

			ch.read(bb);
			ret = bb.array();
		}
		catch(IOException e){
			ret = new byte[0];
		}
		finally{
			try
			{
				if (ch != null)
					ch.close();
			}
			catch(IOException e){}

			try
			{
				if (raf != null)
					raf.close();
			}
			catch(IOException e){}
		}

		return ret;
	}
}

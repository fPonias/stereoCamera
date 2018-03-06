package com.munger.stereocamera.bluetooth.command.master;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;
import com.munger.stereocamera.bluetooth.command.master.listeners.MasterListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothMasterComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;

	public InputStream ins;
	public OutputStream outs;

	OutputProcessor outputProcessor;
	InputProcessor inputProcessor;


	public BluetoothMasterComm(BluetoothCtrl ctrl)
	{
		this.ctrl = ctrl;
		this.socket = ctrl.getMaster().getSocket();

		try
		{
			ins = socket.getInputStream();
			outs = socket.getOutputStream();
		}
		catch (IOException e){
			return;
		}

		inputProcessor = new InputProcessor(this);
		inputProcessor.start();
		outputProcessor = new OutputProcessor(this);
		outputProcessor.start();
	}


	public void cleanUp()
	{
		inputProcessor.cleanUp();
		outputProcessor.cleanUp();
	}

	private byte[] longBuf = new byte[8];
	private ByteBuffer longBufBuf = ByteBuffer.wrap(longBuf);
	public long readLong() throws IOException
	{
		int read;
		do
		{
			read = ins.read(longBuf, 0, 8);
		} while (read == 0);

		if (read < 8)
		{
			throw new IOException("incorrect response");
		}

		long ret = longBufBuf.getLong(0);
		//Log.d("masterReader", "read command arg: " + ret);
		return ret;
	}

	private byte[] intBuf = new byte[4];
	private ByteBuffer intBufBuf = ByteBuffer.wrap(intBuf);
	public int readInt() throws IOException
	{
		int read;
		do
		{
			read = ins.read(intBuf, 0, 4);
		} while (read == 0);

		if (read < 4)
		{
			throw new IOException("incorrect response");
		}

		int ret = intBufBuf.getInt(0);
		//Log.d("masterReader", "read command arg: " + ret);
		return ret;
	}

	public float readFloat() throws IOException
	{
		int read;
		do
		{
			read = ins.read(intBuf, 0, 4);
		} while (read == 0);

		if (read < 4)
		{
			throw new IOException("incorrect response");
		}

		float ret = intBufBuf.getFloat(0);
		String retStr = ret + "";

		if (retStr.contains("NaN"))
		{
			int i = 0;
			int j = i;
		}

		//Log.d("masterReader", "read command arg: " + retStr);
		return ret;
	}

	public void runCommand(MasterCommand command)
	{
		outputProcessor.runCommand(command);
	}

	public void registerListener(MasterListener listener)
	{
		inputProcessor.registerListener(listener);
	}
}

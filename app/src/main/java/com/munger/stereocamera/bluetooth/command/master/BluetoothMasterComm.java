package com.munger.stereocamera.bluetooth.command.master;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;

public class BluetoothMasterComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;

	private InputStream ins;
	private OutputStream outs;

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

	private String getTag() {return "Bluetooth Master Communicator";}

	public void cleanUp()
	{
		inputProcessor.cleanUp();
		outputProcessor.cleanUp();

		try
		{
			outs.close();
		}
		catch(IOException e){}

		try
		{
			ins.close();
		}
		catch(IOException e){}
	}

	public void waitForData() throws IOException
	{
		while(socket.isConnected())
		{
			try
			{
				if (ins == null)
					throw new IOException("wait for data failed on master socket");

				if (ins.available() > 0)
					return;

				try{Thread.sleep(20); } catch(InterruptedException e){}
			}
			catch(IOException e){
				if (!socket.isConnected())
					Log.d(getTag(), "bluetooth socket is closed");

				Log.d(getTag(), "socket closed while waiting for input");
				MyApplication.getInstance().handleDisconnection();

				throw(e);
			}
		}
	}

	public long readLong() throws IOException
	{
		byte[] data = readBytes(8);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getLong(0);
	}

	public int readInt() throws IOException
	{
		byte[] data = readBytes(4);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getInt(0);
	}

	public float readFloat() throws IOException
	{
		byte[] data = readBytes(4);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getFloat(0);
	}

	public byte readByte() throws IOException
	{
		byte[] data = readBytes(1);
		return data[0];
	}

	public byte[] readBytes(int sz) throws IOException
	{
		byte[] data = new byte[sz];

		try
		{
			int total = 0;
			int read = 1;
			while (read > 0 && total < sz)
			{
				if (ins == null)
					throw new IOException("failed master socket read");

				read = ins.read(data, total, sz - total);
				total += read;
			}
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d(getTag(), "bluetooth socket is closed");

			Log.d(getTag(), "failed to read " + sz + "bytes from the master socket");
			MyApplication.getInstance().handleDisconnection();

			throw(e);
		}

		return data;
	}

	public void writeBytes(byte[] array) throws IOException
	{
		try
		{
			if (outs == null)
				throw new IOException("failed master socket write");

			outs.write(array);
			outs.flush();
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d(getTag(), "bluetooth socket is closed");

			Log.d(getTag(), "failed to write " + array.length + "bytes to the master socket");
			MyApplication.getInstance().handleDisconnection();
			throw(e);
		}
	}

	public static class CommandArg
	{
		public MasterCommand command;
		public SlaveListener listener;
	}

	public void runCommand(MasterCommand command, SlaveListener listener)
	{
		CommandArg arg = new CommandArg();
		arg.command = command;
		arg.listener = listener;
		outputProcessor.runCommand(arg);
	}

	public static abstract class SlaveListener
	{
		public abstract void onResponse(MasterIncoming response);
		public void onFail() {}
	}

	public void registerListener(BluetoothCommands command, SlaveListener listener)
	{
		inputProcessor.registerListener(command, listener);
	}
}

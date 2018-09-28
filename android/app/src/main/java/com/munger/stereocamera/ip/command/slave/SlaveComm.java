package com.munger.stereocamera.ip.command.slave;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.command.BluetoothCommands;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class SlaveComm
{
	private SocketCtrl ctrl;
	private Socket socket;
	private InputStream ins;
	private OutputStream outs;

	private CommandProcessor commandProcessor;
	private InputProcessor inputProcessor;
	private OutputProcessor outputProcessor;

	public SlaveComm(SocketCtrl ctrl)
	{
		this.ctrl = ctrl;
		this.socket = ctrl.getSocket();

		try
		{
			if (socket == null)
				throw new IOException("bluetooth slave socket open failed");

			ins = socket.getInputStream();
			outs = socket.getOutputStream();
		}
		catch (IOException e){
			try
			{
				if (ins != null)
					ins.close();
			}catch(NullPointerException|IOException e1){}
			try
			{
				if (outs != null)
					outs.close();
			}catch(NullPointerException|IOException e1){}

			return;
		}

		outputProcessor = new OutputProcessor();
		outputProcessor.start();
		commandProcessor = new CommandProcessor(this);
		commandProcessor.start();
		inputProcessor = new InputProcessor(this);
		inputProcessor.start();
	}

	public void cleanUp()
	{
		try
		{
			if (ins != null)
				ins.close();
		}catch(NullPointerException|IOException e1){}
		try
		{
			if (outs != null)
				outs.close();
		}catch(NullPointerException|IOException e1){}

		ins = null;
		outs = null;

		inputProcessor.cleanUp();
		commandProcessor.cleanUp();
		outputProcessor.cleanUp();
	}

	public interface Listener
	{
		void onCommand(SlaveCommand command);
	}

	public void addListener(BluetoothCommands command, Listener listener)
	{
		commandProcessor.addListener(command, listener);
	}

	public void processCommand(SlaveCommand command)
	{
		command.setComm(this);
		commandProcessor.processCommand(command);
	}

	private boolean pingReceived = false;
	private boolean isSending = false;
	private ConcurrentLinkedQueue<SlaveCommand> commandBacklog = new ConcurrentLinkedQueue<>();
	private final Object lock = new Object();

	public void sendCommand(SlaveCommand command)
	{
		synchronized (lock)
		{
			if (pingReceived)
			{
				commandBacklog.add(command);
			}
			else
			{
				if (command.command != BluetoothCommands.PING)
				{
					commandBacklog.add(command);
					return;
				}
				else
				{
					command.setComm(this);
					outputProcessor.sendCommand(command);
					pingReceived = true;
				}
			}

			if (isSending)
				return;

			isSending = true;
		}

		while (!commandBacklog.isEmpty())
		{
			try
			{
				SlaveCommand cmd = commandBacklog.remove();
				cmd.setComm(this);
				outputProcessor.sendCommand(cmd);
			}
			catch(NoSuchElementException e){}

		}

		synchronized (lock)
		{
			isSending = false;
		}
	}

	private String getTag() {return "Bluetooth Slave Comm";}

	public byte getByte() throws IOException
	{
		byte[] data = getData(1);
		return data[0];
	}

	public void putByte(byte b) throws IOException
	{
		putData(new byte[] {b});
	}

	public int getInt() throws IOException
	{
		byte[] data = getData(4);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getInt(0);
	}

	public float getFloat() throws IOException
	{
		byte[] data = getData(4);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getFloat(0);
	}

	private ByteBuffer ointbb = ByteBuffer.allocate(4);
	public void putInt(int val) throws IOException
	{
		ointbb.putInt(0, val);
		putData(ointbb.array());
	}

	public void putFloat(float val) throws IOException
	{
		ointbb.putFloat(0, val);
		putData(ointbb.array());
	}

	private ByteBuffer olongbb = ByteBuffer.allocate(8);
	public void putLong(long val) throws IOException
	{
		olongbb.putLong(0, val);
		putData(olongbb.array());
	}

	public void putData(byte[] arr) throws IOException
	{
		try
		{
			if (outs == null)
				throw new IOException("slave socket write error");

			outs.write(arr);
			outs.flush();
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d(getTag(), "slave socket no longer open");

			Log.d(getTag(), "slave failed to send array of size " + arr.length);

			MainActivity.getInstance().handleDisconnection();
			throw e;
		}
	}

	public byte[] getData(int sz) throws IOException
	{
		try
		{
			byte[] ret = new byte[sz];
			int total = 0;
			int read = 1;

			while (read > 0 && total < sz)
			{
				if (ins == null)
					throw new IOException("slave socket read error");

				read = ins.read(ret, total, sz - total);
				total += read;
			}

			return ret;
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d(getTag(), "slave socket no longer open");

			Log.d(getTag(), "slave failed to read array of size " + sz);

			MainActivity.getInstance().handleDisconnection();
			throw e;
		}
	}

	public void pipeData(int sz, OutputStream ostr) throws IOException
	{
		try
		{
			byte[] buffer = new byte[1024];
			int read = 1;
			int total = 0;

			while (read > 0 && total < sz)
			{
				int toRead = Math.min(1024, sz - total);
				read = ins.read(buffer, 0, toRead);

				if (read > 0)
				{
					ostr.write(buffer, 0, read);
					total += read;
				}
			}
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d(getTag(), "slave socket no longer open");

			Log.d(getTag(), "slave failed to pipe output");

			MainActivity.getInstance().handleDisconnection();
			throw e;
		}
	}
}

package com.munger.stereocamera.ip.command;

import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class Comm
{
	private String getTag() {return "General Comm";}
	private Socket socket;
	private OutputStream outs;
	private InputStream ins;

	public Comm(SocketCtrl ctrl) throws IOException
	{
		socket = ctrl.getSocket();
		outs = ctrl.getSocket().getOutputStream();
		ins = ctrl.getSocket().getInputStream();
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

	public byte getByte() throws IOException
	{
		byte[] data = getData(1);
		return data[0];
	}

	public void putByte(byte b) throws IOException
	{
		putData(new byte[] {b});
	}

	public boolean getBoolean() throws IOException
	{
		byte data = getByte();
		return (data > 0) ? true : false;
	}

	public void putBoolean(boolean value) throws IOException
	{
		byte data = (value) ? (byte) 1 : (byte) 0;
		putByte(data);
	}

	public int getInt() throws IOException
	{
		byte[] data = getData(4);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getInt(0);
	}

	public long getLong() throws IOException
	{
		byte[] data = getData(8);
		ByteBuffer bb = ByteBuffer.wrap(data);
		return bb.getLong(0);
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
}

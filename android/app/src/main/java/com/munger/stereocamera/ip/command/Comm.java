package com.munger.stereocamera.ip.command;

import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.Socket;
import com.munger.stereocamera.ip.SocketCtrl;

import java.io.File;
import java.io.FileInputStream;
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

	public void putStream(InputStream fis, long sz) throws IOException
	{
		try
		{
			byte[] buf = new byte[4096];
			long count = 0;
			long read = 0;

			while (read > -1 && count < sz)
			{
				long diff = sz - count;
				long toRead = Math.min(diff, 4096);
				read = fis.read(buf, 0, (int) toRead);

				count += read;
				outs.write(buf, 0, (int) toRead);
			}

			outs.flush();
		}
		catch (IOException e)
		{
			if (!socket.isConnected())
				Log.d("stereoCamera", "slave socket no longer open");

			Log.d("stereoCamera", "slave failed to send file of size " + sz);

			MainActivity.getInstance().handleDisconnection();
			throw e;
		}
	}

	public void putFile(File file) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		long sz = file.length();

		putStream(fis, sz);
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
				Log.d("stereoCamera", "slave socket no longer open");

			Log.d("stereoCamera", "slave failed to send array of size " + arr.length);

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

			Log.d("stereoCamera", "comm read " + total + " bytes");
			if (total == -1)
				throw new IOException("slave socket read error");

			return ret;
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d("stereoCamera", "slave socket no longer open");

			Log.d("stereoCamera", "slave failed to read array of size " + sz);

			MainActivity.getInstance().handleDisconnection();
			throw e;
		}
	}

	public void pipeData(int sz, OutputStream pipe) throws IOException
	{
		byte[] buf = new byte[4096];
		int total = 0;
		int read = 0;

		try
		{
			while (total < sz)
			{
				int diff = sz - total;
				int toRead = Math.min(diff, 4096);

				read = ins.read(buf, 0, toRead);
				pipe.write(buf, 0, read);
				total += read;
			}
		}
		catch(IOException e){
			if (!socket.isConnected())
				Log.d("stereoCamera", "slave socket no longer open");

			Log.d("stereoCamera", "slave failed to send file of size " + sz);

			MainActivity.getInstance().handleDisconnection();
			throw e;
		}

		Log.d("stereoCamera", "piped " + total + " bytes");
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

	//bytes are reversed for long and int to accomodate ios integer encoding.
	public int getInt() throws IOException
	{
		byte[] data = getData(4);
		ByteBuffer bb = ByteBuffer.wrap(data);
		int ret = bb.getInt(0);
		return Integer.reverseBytes(ret);
	}

	public long getLong() throws IOException
	{
		byte[] data = getData(8);
		ByteBuffer bb = ByteBuffer.wrap(data);
		long ret = bb.getLong(0);
		return Long.reverseBytes(ret);
	}

	private void swapBytes(byte[] data)
	{
		int sz = data.length;
		int half = sz / 2;
		for (int i = 0; i < half; i++)
		{
			byte b = data[sz - 1 - i];
			data[sz - 1 - i] = data[i];
			data[i] = b;
		}
	}

	public float getFloat() throws IOException
	{
		byte[] data = getData(4);
		swapBytes(data);

		ByteBuffer bb = ByteBuffer.wrap(data);
		float ret = bb.getFloat(0);
		return ret;
	}

	private ByteBuffer ointbb = ByteBuffer.allocate(4);
	private int iNewVal;
	public void putInt(int val) throws IOException
	{
		iNewVal = Integer.reverseBytes(val);
		ointbb.putInt(0, iNewVal);
		putData(ointbb.array());
	}

	public void putFloat(float val) throws IOException
	{
		ointbb.putFloat(0, val);
		byte[] arr = ointbb.array();
		swapBytes(arr);
		putData(arr);
	}

	private ByteBuffer olongbb = ByteBuffer.allocate(8);
	private long lNewVal;
	public void putLong(long val) throws IOException
	{
		lNewVal = Long.reverseBytes(val);
		olongbb.putLong(0, lNewVal);
		putData(olongbb.array());
	}


}

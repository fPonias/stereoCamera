package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothMasterComm
{
	private BluetoothCtrl ctrl;
	private BluetoothSocket socket;
	private InputStream ins;
	private OutputStream outs;
	private byte[] buffer;
	private static final int BUFFER_SIZE = 4096;

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

		}

		buffer = new byte[BUFFER_SIZE];
	}

	public interface PingListener
	{
		void pong(long diff);
		void fail();
	}

	public void ping(final PingListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			try
			{
				ping2(listener);
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	private void ping2(PingListener listener) throws IOException
	{
		long now = System.currentTimeMillis();
		sendCommand(BluetoothComm.PING);

		long then = System.currentTimeMillis();
		long diff = then - now;

		listener.pong(diff);
	}

	public interface ShutterListener
	{
		void onData(byte[] data);
		void fail();
	}

	public void shutter(final ShutterListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			try
			{
				shutter2(listener);
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	private void shutter2(ShutterListener listener) throws IOException
	{
		sendCommand(BluetoothComm.FIRE_SHUTTER);

		int size = readInt();
		ByteBuffer imgBuffer = ByteBuffer.allocate(size);
		int sz = 0;

		int read;
		while (sz < size)
		{
			read = ins.read(buffer);
			sz += read;
			imgBuffer.put(buffer, 0 , read);
		}

		listener.onData(imgBuffer.array());
	}

	public interface LatencyCheckListener
	{
		void trigger(LatencyCheckListenerListener listener);
		void pong(long localLatency, long remoteLatency);
		void fail();
	}

	public interface LatencyCheckListenerListener
	{
		void reply();
	}

	private long localLatency;
	private long remoteLatency;
	private final Object latencyLock = new Object();

	public void latencyCheck(final LatencyCheckListener listener)
	{
		final long now = System.currentTimeMillis();
		localLatency = -1;
		remoteLatency = -1;

		listener.trigger(new LatencyCheckListenerListener() { public void reply()
		{
			synchronized (latencyLock)
			{
				localLatency = System.currentTimeMillis() - now;

				if (remoteLatency != -1)
					latencyLock.notify();
			}
		}});

		Thread t = new Thread(new Runnable() { public void run()
		{
			long ret;
			try
			{
				ret = latencyCheck2(listener);
			}
			catch(IOException e){
				listener.fail();
				return;
			}

			synchronized (latencyLock)
			{
				remoteLatency = ret;

				if (localLatency != -1)
					latencyLock.notify();
			}
		}});
		t.start();

		Thread foo = new Thread(new Runnable() {public void run()
		{
			synchronized (latencyLock)
			{
				try{latencyLock.wait(3000);}catch(InterruptedException e){}

				if (localLatency == -1 || remoteLatency == -1)
					listener.fail();
				else
					listener.pong(localLatency, remoteLatency);
			}
		}});
		foo.start();
	}

	private long latencyCheck2(LatencyCheckListener listener) throws IOException
	{
		sendCommand(BluetoothComm.LATENCY_CHECK);

		long ret = readLong();
		return ret;
	}

	public interface FlipListener
	{
		void done();
		void fail();
	}

	public void flip(final FlipListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			try
			{
				sendCommand(BluetoothComm.FLIP);
				listener.done();
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	public interface StatusListener
	{
		void done(int status);
		void fail();
	}

	public void getStatus(final StatusListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			try
			{
				sendCommand(BluetoothComm.GET_STATUS);
				int ret = readInt();

				listener.done(ret);
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	public interface AngleOfViewListener
	{
		void done(float horiz, float vert);
		void fail();
	}

	public void getAngleOfView(final AngleOfViewListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			float[] ret = new float[2];
			try
			{
				sendCommand(BluetoothComm.GET_ANGLE_OF_VIEW);

				for (int i = 0; i < 2; i++)
				{
					ret[i] = readFloat();
				}

				listener.done(ret[0], ret[1]);
			}
			catch(IOException e)
			{
				listener.fail();
			}
		}});
		t.start();
	}

	public interface ZoomListener
	{
		void done();
		void fail();
	}

	public void setZoom(final float zoom, final ZoomListener listener)
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			byte[] args = ByteBuffer.allocate(4).putFloat(zoom).array();
			try
			{
				sendCommand(BluetoothComm.SET_ZOOM, args);
			}
			catch(IOException e){
				listener.fail();
			}
		}});
		t.start();
	}

	private void sendCommand(int command) throws IOException
	{
		sendCommand(command, null);
	}

	private void sendCommand(int command, byte[] args) throws IOException
	{
		byte[] bytes = ByteBuffer.wrap(intBuf).putInt(command).array();
		outs.write(bytes);

		if (args != null)
			outs.write(args);

		outs.flush();

		int action = readInt();

		if (action != command)
		{
			throw new IOException("incorrect response");
		}
	}

	public void cleanUp()
	{
	}

	private byte[] longBuf = new byte[8];
	private ByteBuffer longBufBuf = ByteBuffer.wrap(longBuf);
	private long readLong() throws IOException
	{
		int read;
		do
		{
			read = ins.read(longBuf);
		} while (read == 0);

		if (read < 8)
		{
			throw new IOException("incorrect response");
		}

		return longBufBuf.getLong();
	}

	private byte[] intBuf = new byte[4];
	private ByteBuffer intBufBuf = ByteBuffer.wrap(intBuf);
	private int readInt() throws IOException
	{
		int read;
		do
		{
			read = ins.read(intBuf);
		} while (read == 0);

		if (read < 4)
		{
			throw new IOException("incorrect response");
		}

		return intBufBuf.getInt();
	}

	private float readFloat() throws IOException
	{
		int read;
		do
		{
			read = ins.read(intBuf);
		} while (read == 0);

		if (read < 4)
		{
			throw new IOException("incorrect response");
		}

		return intBufBuf.getFloat();
	}
}

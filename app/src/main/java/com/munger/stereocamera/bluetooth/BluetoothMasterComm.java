package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.PING.name() + " success");
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.PING.name() + " failed");
				listener.fail();
			}
		}});
		t.start();
	}

	private void ping2(PingListener listener) throws IOException
	{
		long now = System.currentTimeMillis();
		sendCommand(BluetoothCommands.PING);

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
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FIRE_SHUTTER.name() + " success");
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FIRE_SHUTTER.name() + " failed");
				listener.fail();
			}
		}});
		t.start();
	}

	private void shutter2(ShutterListener listener) throws IOException
	{
		sendCommand(BluetoothCommands.FIRE_SHUTTER);

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
		Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " called");

		listener.trigger(new LatencyCheckListenerListener() { public void reply()
		{
			synchronized (latencyLock)
			{
				localLatency = System.currentTimeMillis() - now;

				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " local check success");
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

				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " remote check success");
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " remote check failed");
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
				{
					Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " failed");
					listener.fail();
				}
				else
				{
					Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.LATENCY_CHECK.name() + " success");
					listener.pong(localLatency, remoteLatency);
				}
			}
		}});
		foo.start();
	}

	private long latencyCheck2(LatencyCheckListener listener) throws IOException
	{
		sendCommand(BluetoothCommands.LATENCY_CHECK);

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
				sendCommand(BluetoothCommands.FLIP);

				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FLIP.name() + " success");
				listener.done();
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.FLIP.name() + " failed");
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
				sendCommand(BluetoothCommands.GET_STATUS);
				int ret = readInt();

				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_STATUS.name() + " success");
				listener.done(ret);
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_STATUS.name() + " failed");
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
				sendCommand(BluetoothCommands.GET_ANGLE_OF_VIEW);

				for (int i = 0; i < 2; i++)
				{
					ret[i] = readFloat();
				}

				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_ANGLE_OF_VIEW.name() + " success");
				listener.done(ret[0], ret[1]);
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.GET_ANGLE_OF_VIEW.name() + " failed");
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
				sendCommand(BluetoothCommands.SET_ZOOM, args);
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.SET_ZOOM.name() + " success");
				listener.done();
			}
			catch(IOException e){
				Log.d("BluetoothMasterComm", "command: " + BluetoothCommands.SET_ZOOM.name() + " failed");
				listener.fail();
			}
		}});
		t.start();
	}

	private void sendCommand(BluetoothCommands command) throws IOException
	{
		sendCommand(command, null);
	}

	private void sendCommand(BluetoothCommands command, byte[] args) throws IOException
	{
		Log.d("BluetoothMasterComm", "command: " + command.name() + " started");
		byte[] bytes = ByteBuffer.wrap(intBuf).putInt(command.ordinal()).array();
		outs.write(bytes);

		if (args != null)
			outs.write(args);

		outs.flush();

		int actionInt = readInt();
		BluetoothCommands action = BluetoothCommands.values()[actionInt];

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
			read = ins.read(longBuf, 0, 8);
		} while (read == 0);

		if (read < 8)
		{
			throw new IOException("incorrect response");
		}

		return longBufBuf.getLong(0);
	}

	private byte[] intBuf = new byte[4];
	private ByteBuffer intBufBuf = ByteBuffer.wrap(intBuf);
	private int readInt() throws IOException
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

		return intBufBuf.getInt(0);
	}

	private float readFloat() throws IOException
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

		return intBufBuf.getFloat(0);
	}
}

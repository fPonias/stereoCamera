package com.munger.stereocamera.ip.command;

import com.munger.stereocamera.ip.SocketCtrl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Command
{
	protected static int nextId = 1;

	public enum Type
	{
		NONE,
		FIRE_SHUTTER,
		PING,
		LATENCY_CHECK,
		RECEIVE_ANGLE_OF_VIEW,
		SET_ZOOM,
		SET_FACING,
		SET_OVERLAY,
		SEND_VERSION,
		RECEIVE_ZOOM,
		RECEIVE_STATUS,
		RECEIVE_GRAVITY,
		RECEIVE_ORIENTATION,
		CONNECTION_PAUSE,
		DISCONNECT,
		SEND_PROCESSED_PHOTO,
		RECEIVE_PREVIEW_FRAME,
		HANDSHAKE,
		SET_CAPTURE_QUALITY,
		ID
	}

	public Type cmdtype = Type.NONE;
	public int id;
	public boolean isResponse = false;

	protected boolean expectsResponse = false;

	public Command()
	{
		id = Command.nextId;
		Command.nextId++;
	}

	public boolean send(Comm comm)
	{
		try
		{
			comm.putByte((byte) cmdtype.ordinal());
			comm.putLong(id);
			comm.putByte((byte) ((isResponse) ? 1 : 0));
		}
		catch (IOException e){
			return false;
		}

		return true;
	}

	public boolean receive(Comm comm)
	{
		try
		{
			id = (int) comm.getLong();
			isResponse = comm.getBoolean();
		}
		catch(IOException e){
			return false;
		}

		return true;
	}

	public void onResponse(Command command)
	{}
}

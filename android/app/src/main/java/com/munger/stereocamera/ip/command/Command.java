package com.munger.stereocamera.ip.command;

import com.munger.stereocamera.ip.SocketCtrl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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
		RECEIVE_CONNECTION_PAUSE,
		DISCONNECT,
		RECEIVE_DISCONNECT,
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write((byte) cmdtype.ordinal());
		baos.write(id);
		baos.write((byte) ((isResponse) ? 1 : 0));

		try
		{
			comm.putData(baos.toByteArray());
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
			comm.getData(9);
			id = comm.getInt();
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

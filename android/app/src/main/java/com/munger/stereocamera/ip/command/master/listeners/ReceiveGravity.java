package com.munger.stereocamera.ip.command.master.listeners;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.MasterIncoming;

import java.io.IOException;


public class ReceiveGravity extends MasterIncoming
{
	public Gravity gravity;

	public ReceiveGravity()
	{
		super(Command.Type.RECEIVE_GRAVITY, -1);
	}

	@Override
	public void readResponse() throws IOException
	{
		Gravity ret = new Gravity();
		ret.x = parent.readFloat();
		ret.y = parent.readFloat();
		ret.z = parent.readFloat();
		gravity = ret;
	}

	public static class Gravity
	{
		public float x;
		public float y;
		public float z;
	}
}

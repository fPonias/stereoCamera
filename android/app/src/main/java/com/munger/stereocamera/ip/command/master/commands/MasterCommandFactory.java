package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.Command;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class MasterCommandFactory
{
	private static HashMap<Command.Type, Class> classMap;

	private static void populateClassMap()
	{
		classMap = new HashMap<>();
		classMap.put(Command.Type.CONNECTION_PAUSE, ConnectionPause.class);
		classMap.put(Command.Type.DISCONNECT, Disconnect.class);
		classMap.put(Command.Type.LATENCY_CHECK, GetRemoteLatency.class);
		classMap.put(Command.Type.PING, Ping.class);
		classMap.put(Command.Type.SEND_PROCESSED_PHOTO, SendProcessedPhoto.class);
		classMap.put(Command.Type.SET_FACING, SetFacing.class);
		classMap.put(Command.Type.SET_OVERLAY, SetOverlay.class);
		classMap.put(Command.Type.SET_ZOOM, SetZoom.class);
		classMap.put(Command.Type.FIRE_SHUTTER, Shutter.class);
		classMap.put(Command.Type.HANDSHAKE, Handshake.class);
	}

	public static MasterCommand get(Command.Type command, int id)
	{
		if (classMap == null)
			populateClassMap();

		Class cls = classMap.get(command);

		if (cls == null)
			return null;

		try
		{
			Constructor<?> constr = cls.getConstructor(Command.Type.class, Integer.class);
			MasterCommand ret = (MasterCommand) constr.newInstance(command, id);
			return ret;
		}
		catch(NoSuchMethodException e){
			return null;
		}
		catch(IllegalAccessException e1){
			return null;
		}
		catch(InvocationTargetException e2){
			return null;
		}
		catch (InstantiationException e3){
			return null;
		}
	}
}


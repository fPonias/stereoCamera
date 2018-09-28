package com.munger.stereocamera.ip.command.master.commands;

import com.munger.stereocamera.ip.command.BluetoothCommands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class MasterCommandFactory
{
	private static HashMap<BluetoothCommands, Class> classMap;

	private static void populateClassMap()
	{
		classMap = new HashMap<>();
		classMap.put(BluetoothCommands.CONNECTION_PAUSE, ConnectionPause.class);
		classMap.put(BluetoothCommands.DISCONNECT, Disconnect.class);
		classMap.put(BluetoothCommands.LATENCY_CHECK, GetRemoteLatency.class);
		classMap.put(BluetoothCommands.PING, Ping.class);
		classMap.put(BluetoothCommands.SEND_PROCESSED_PHOTO, SendProcessedPhoto.class);
		classMap.put(BluetoothCommands.SET_FACING, SetFacing.class);
		classMap.put(BluetoothCommands.SET_OVERLAY, SetOverlay.class);
		classMap.put(BluetoothCommands.SET_ZOOM, SetZoom.class);
		classMap.put(BluetoothCommands.FIRE_SHUTTER, Shutter.class);
		classMap.put(BluetoothCommands.HANDSHAKE, Handshake.class);
	}

	public static MasterCommand get(BluetoothCommands command, int id)
	{
		if (classMap == null)
			populateClassMap();

		Class cls = classMap.get(command);

		if (cls == null)
			return null;

		try
		{
			Constructor<?> constr = cls.getConstructor(BluetoothCommands.class, Integer.class);
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


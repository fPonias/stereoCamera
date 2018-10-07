package com.munger.stereocamera.ip.command.master;

import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveAngleOfView;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveConnectionPause;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveDisconnect;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveOrientation;
import com.munger.stereocamera.ip.command.master.listeners.ReceivePreviewFrame;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveStatus;
import com.munger.stereocamera.ip.command.master.listeners.ReceiveZoom;

import java.util.HashMap;

/**
 * Created by hallmarklabs on 3/29/18.
 */

public class InputProcessorFactory
{
	private static HashMap<Command.Type, Class> classMap;

	private static void populateClassMap()
	{
		classMap = new HashMap<>();
		classMap.put(Command.Type.RECEIVE_ANGLE_OF_VIEW, ReceiveAngleOfView.class);
		classMap.put(Command.Type.RECEIVE_CONNECTION_PAUSE, ReceiveConnectionPause.class);
		classMap.put(Command.Type.RECEIVE_DISCONNECT, ReceiveDisconnect.class);
		classMap.put(Command.Type.RECEIVE_GRAVITY, ReceiveGravity.class);
		classMap.put(Command.Type.RECEIVE_ORIENTATION, ReceiveOrientation.class);
		classMap.put(Command.Type.RECEIVE_PREVIEW_FRAME, ReceivePreviewFrame.class);
		classMap.put(Command.Type.RECEIVE_STATUS, ReceiveStatus.class);
		classMap.put(Command.Type.RECEIVE_ZOOM, ReceiveZoom.class);
	}

	public static MasterIncoming getCommand(Command.Type command)
	{
		if (classMap == null)
			populateClassMap();

		Class cls = classMap.get(command);

		if (cls == null)
			return null;

		try
		{
			MasterIncoming ret = (MasterIncoming) cls.newInstance();
			return ret;
		}
		catch(IllegalAccessException e1){
			return null;
		}
		catch (InstantiationException e3){
			return null;
		}
	}
}

package com.munger.stereocamera.bluetooth.command.master;

import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveAngleOfView;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveAudioSyncTriggered;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveConnectionPause;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveDisconnect;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveOrientation;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceivePreviewFrame;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveStatus;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveZoom;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Created by hallmarklabs on 3/29/18.
 */

public class InputProcessorFactory
{
	private static HashMap<BluetoothCommands, Class> classMap;

	private static void populateClassMap()
	{
		classMap = new HashMap<>();
		classMap.put(BluetoothCommands.RECEIVE_ANGLE_OF_VIEW, ReceiveAngleOfView.class);
		classMap.put(BluetoothCommands.RECEIVE_CONNECTION_PAUSE, ReceiveConnectionPause.class);
		classMap.put(BluetoothCommands.RECEIVE_DISCONNECT, ReceiveDisconnect.class);
		classMap.put(BluetoothCommands.RECEIVE_GRAVITY, ReceiveGravity.class);
		classMap.put(BluetoothCommands.RECEIVE_ORIENTATION, ReceiveOrientation.class);
		classMap.put(BluetoothCommands.RECEIVE_PREVIEW_FRAME, ReceivePreviewFrame.class);
		classMap.put(BluetoothCommands.RECEIVE_STATUS, ReceiveStatus.class);
		classMap.put(BluetoothCommands.RECEIVE_ZOOM, ReceiveZoom.class);
		classMap.put(BluetoothCommands.RECEIVE_AUDIO_SYNC_TRIGGERED, ReceiveAudioSyncTriggered.class);
	}

	public static MasterIncoming getCommand(BluetoothCommands command)
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

package com.munger.stereocamera.ip.command;

import android.util.Log;

import com.munger.stereocamera.ip.command.commands.ConnectionPause;
import com.munger.stereocamera.ip.command.commands.Disconnect;
import com.munger.stereocamera.ip.command.commands.FireShutter;
import com.munger.stereocamera.ip.command.commands.Handshake;
import com.munger.stereocamera.ip.command.commands.ID;
import com.munger.stereocamera.ip.command.commands.LatencyTest;
import com.munger.stereocamera.ip.command.commands.Ping;
import com.munger.stereocamera.ip.command.commands.SendGravity;
import com.munger.stereocamera.ip.command.commands.SendPhoto;
import com.munger.stereocamera.ip.command.commands.SendStatus;
import com.munger.stereocamera.ip.command.commands.SendZoom;
import com.munger.stereocamera.ip.command.commands.SetCaptureQuality;
import com.munger.stereocamera.ip.command.commands.SetFacing;
import com.munger.stereocamera.ip.command.commands.SetOverlay;
import com.munger.stereocamera.ip.command.commands.SetZoom;
import com.munger.stereocamera.ip.command.commands.Version;

public class CommandFactory
{
	public static class DefaultCommand extends Command
	{}

	static Command build(Command.Type type)
	{
		switch (type)
		{
			case PING:
				return new Ping();
			case HANDSHAKE:
				return new Handshake();
			case RECEIVE_STATUS:
				return new SendStatus();
			case SEND_VERSION:
				return new Version();
			case SET_FACING:
				return new SetFacing();
			case SET_OVERLAY:
				return new SetOverlay();
			case FIRE_SHUTTER:
				return new FireShutter();
			case SEND_PROCESSED_PHOTO:
				return new SendPhoto();
			case SET_ZOOM:
				return new SetZoom();
			case RECEIVE_ZOOM:
				return new SendZoom();
			case DISCONNECT:
				return new Disconnect();
			case CONNECTION_PAUSE:
				return new ConnectionPause();
			case RECEIVE_GRAVITY:
				return new SendGravity();
			case SET_CAPTURE_QUALITY:
				return new SetCaptureQuality();
			case LATENCY_CHECK:
				return new LatencyTest();
			case ID:
				return new ID();
			default:
				Log.w("stereoCamera", "Warning: default command generated");
				return new DefaultCommand();
		}
	}
}

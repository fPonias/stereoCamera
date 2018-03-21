package com.munger.stereocamera.bluetooth.command;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public enum BluetoothCommands
{
	NONE,
	FIRE_SHUTTER,
	PING,
	LATENCY_CHECK,
	RECEIVE_ANGLE_OF_VIEW,
	SET_ZOOM,
	SET_FACING,
	SET_OVERLAY,
	RECEIVE_ZOOM,
	RECEIVE_STATUS,
	RECEIVE_GRAVITY,
	RECEIVE_ORIENTATION,
	DISCONNECT,
	RECEIVE_DISCONNECT,
	SEND_PROCESSED_PHOTO
}

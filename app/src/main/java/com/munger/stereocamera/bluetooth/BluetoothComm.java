package com.munger.stereocamera.bluetooth;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class BluetoothComm
{
	public static final int FIRE_SHUTTER = 0x01;
	public static final int PING = 0x02;
	public static final int LATENCY_CHECK = 0x04;
	public static final int FLIP = 0x08;
	public static final int GET_ANGLE_OF_VIEW = 0x0f;
	public static final int SET_ZOOM = 0x10;
	public static final int GET_STATUS = 0x20;
}

package com.munger.stereocamera.bluetooth.command;

/**
 * Created by hallmarklabs on 3/7/18.
 */

public enum PhotoOrientation
{
	DEG_0,
	DEG_90,
	DEG_180,
	DEG_270;

	public double toDegress()
	{
		switch (this)
		{
			case DEG_0: return 0;
			case DEG_90: return 90;
			case DEG_180: return 180;
			case DEG_270: return 270;
			default: return 0;
		}
	}

	public double toRadians()
	{
		switch (this)
		{
			case DEG_0: return 0;
			case DEG_90: return Math.PI * 0.5;
			case DEG_180: return Math.PI;
			case DEG_270: return 1.5 * Math.PI;
			default: return 0;
		}
	}

	public boolean isPortait()
	{
		switch(this)
		{
			case DEG_0:
			case DEG_180:
				return true;
			case DEG_90:
			case DEG_270:
			default:
				return false;
		}
	}
}

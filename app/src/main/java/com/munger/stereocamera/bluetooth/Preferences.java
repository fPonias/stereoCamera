package com.munger.stereocamera.bluetooth;

import android.content.Context;
import android.content.SharedPreferences;

import com.munger.stereocamera.MyApplication;

/**
 * Created by hallmarklabs on 3/4/18.
 */

public class Preferences
{
	public enum Roles
	{
		MASTER,
		SLAVE,
		NONE
	};

	private static final String ROLE_KEY = "role";
	private static final String CLIENT_KEY = "client";
	private static final String SHUTTER_DELAY_KEY = "shutterDelay";
	private static final String LOCAL_ZOOM_KEY = "localZoom";
	private static final String REMOTE_ZOOM_KEY = "remoteZoom";
	private static final String SIDE_KEY = "side";
	private static final String FACING_KEY = "facing";

	private SharedPreferences preferences;

	private Roles role = Roles.NONE;
	private String client = null;
	private double shutterDelay = 0.0;
	private int localZoom = 1;
	private float remoteZoom = 1;
	private boolean isOnLeft = true;
	private boolean isFacing = true;

	public void setup()
	{
		readPreferences();
	}

	private void readPreferences()
	{
		preferences = MyApplication.getInstance().getCurrentActivity().getPreferences(Context.MODE_PRIVATE);

		if (preferences.contains(ROLE_KEY))
		{
			int value = preferences.getInt(ROLE_KEY, -1);
			role = Roles.values()[value];
		}
		else
		{
			role = Roles.NONE;
		}

		if (preferences.contains(CLIENT_KEY))
		{
			String value = preferences.getString(CLIENT_KEY, null);
			client = value;
		}
		else
		{
			client = null;
		}

		if (preferences.contains(SHUTTER_DELAY_KEY))
		{
			float value = preferences.getFloat(SHUTTER_DELAY_KEY, 0.0f);
			shutterDelay = value;
		}

		if (preferences.contains(LOCAL_ZOOM_KEY))
		{
			int value = preferences.getInt(LOCAL_ZOOM_KEY, 0);
			localZoom = value;
		}

		if (preferences.contains(REMOTE_ZOOM_KEY))
		{
			float value = preferences.getFloat(REMOTE_ZOOM_KEY, 1.0f);
			remoteZoom = value;
		}

		if (preferences.contains(SIDE_KEY))
		{
			boolean value = preferences.getBoolean(SIDE_KEY, true);
			isOnLeft = value;
		}

		if (preferences.contains(FACING_KEY))
		{
			boolean value = preferences.getBoolean(FACING_KEY, true);
			isFacing = value;
		}
	}

	public Roles getRole()
	{
		return role;
	}

	public void setRole(Roles role)
	{
		this.role = role;

		preferences.edit().putInt(ROLE_KEY, role.ordinal()).apply();
	}

	public String getClient()
	{
		return client;
	}

	public void setClient(String client)
	{
		this.client = client;

		preferences.edit().putString(CLIENT_KEY, client).apply();
	}

	public double getShutterDelay()
	{
		return shutterDelay;
	}

	public void setShutterDelay(double value)
	{
		shutterDelay = value;

		preferences.edit().putFloat(SHUTTER_DELAY_KEY, (float) value).apply();
	}

	public int getLocalZoom()
	{
		return localZoom;
	}

	public void setLocalZoom(int index)
	{
		localZoom = index;
		preferences.edit().putInt(LOCAL_ZOOM_KEY, index).apply();
	}

	public float getRemoteZoom()
	{
		return remoteZoom;
	}

	public void setRemoteZoom(float value)
	{
		remoteZoom = value;
		preferences.edit().putFloat(REMOTE_ZOOM_KEY, value).apply();
	}

	public boolean getIsOnLeft()
	{
		return isOnLeft;
	}

	public void setIsOnLeft(boolean side)
	{
		isOnLeft = side;
		preferences.edit().putBoolean(SIDE_KEY, side).apply();
	}

	public boolean getIsFacing()
	{
		return isFacing;
	}

	public void setIsFacing(boolean facing)
	{
		isFacing = facing;
		preferences.edit().putBoolean(FACING_KEY, facing).apply();
	}
}

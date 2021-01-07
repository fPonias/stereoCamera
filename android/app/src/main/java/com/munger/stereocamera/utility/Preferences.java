package com.munger.stereocamera.utility;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


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
	private static final String SIDE_KEY = "side";
	private static final String FACING_KEY = "facing";
	private static final String FIRST_TIME = "first_time";
	private static final String ID = "id";
	private static final String PROCESSOR_TYPES = "pref_formats";
	private static final String OVERLAY = "pref_overlay";
	private static final String RESOLUTION = "pref_capture";

	private enum CameraKeysEnum
	{
		SHUTTER_DELAY_KEY,
		LOCAL_ZOOM_KEY,
		REMOTE_ZOOM_KEY
	};

	private class cameraPrefs
	{
		public cameraPrefs(String cameraId)
		{
			this.cameraId = cameraId;
		}

		public String cameraId;

		public double shutterDelay = 0.0;
		public float localZoom = 1;
		public float remoteZoom = 1;

		public String getKey(CameraKeysEnum key)
		{
			return key.name() + "_" + cameraId;
		}
	}

	private SharedPreferences preferences;

	private Roles role = Roles.NONE;
	private String client = null;
	private boolean isOnLeft = true;
	private boolean isFacing = true;
	private boolean firstTime = true;
	private HashMap<String, cameraPrefs> cameras = new HashMap<>();
	private Set<PhotoProcessor.CompositeImageType> processorTypes;
	private PreviewOverlayWidget.Type overlay;
	private PreviewWidget.SHUTTER_TYPE resolution;
	private String id = null;

	public Preferences()
	{
		cameras.put("0", new cameraPrefs("0"));
		cameras.put("1", new cameraPrefs("1"));
	}

	public void setup()
	{
		readPreferences();
	}

	public void updateSettingsPrefs()
	{
		updateProcessorTypes();
		updateOverlay();
		updateResolution();
	}

	private void readPreferences()
	{
		preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());

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


		for (String key : cameras.keySet())
		{
			cameraPrefs prefs = cameras.get(key);

			String prefKey = prefs.getKey(CameraKeysEnum.LOCAL_ZOOM_KEY);
			if (preferences.contains(prefKey))
			{
				float value = preferences.getFloat(prefKey, 1.0f);
				prefs.localZoom = value;
			}

			prefKey = prefs.getKey(CameraKeysEnum.REMOTE_ZOOM_KEY);
			if (preferences.contains(prefKey))
			{
				float value = preferences.getFloat(prefKey, 1.0f);
				prefs.remoteZoom = value;
			}

			prefKey = prefs.getKey(CameraKeysEnum.SHUTTER_DELAY_KEY);
			if (preferences.contains(prefKey))
			{
				double value = preferences.getFloat(prefKey, 0.0f);
				prefs.shutterDelay = value;
			}
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

		if (preferences.contains(FIRST_TIME))
		{
			boolean value = preferences.getBoolean(FIRST_TIME, true);
			firstTime = value;
		}

		id = preferences.getString(ID, null);

		if (id == null)
		{
			UUID uuid = UUID.randomUUID();
			id = uuid.toString();
			preferences.edit().putString(ID, id).apply();
		}

		updateSettingsPrefs();
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

	private void updateProcessorTypes()
	{
		Set<String> def =  Collections.singleton("split");
		if (!preferences.contains(PROCESSOR_TYPES))
			preferences.edit().putStringSet(PROCESSOR_TYPES, def).apply();

		Set<String> vals = preferences.getStringSet(PROCESSOR_TYPES, def);

		processorTypes = new HashSet<>();
		for (String val : vals)
			processorTypes.add(PhotoProcessor.getType(val));
	}

	private void updateOverlay()
	{
		String val = preferences.getString(OVERLAY, "0");
		int valIdx = Integer.parseInt(val);
		overlay = PreviewOverlayWidget.Type.values()[valIdx];
	}

	private void updateResolution()
	{
		String val = preferences.getString(RESOLUTION, "preview");
		if (val.equals("preview"))
			resolution = PreviewWidget.SHUTTER_TYPE.PREVIEW;
		else
			resolution = PreviewWidget.SHUTTER_TYPE.HI_RES;
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

	public double getShutterDelay(String cameraId)
	{
		cameraPrefs prefs = cameras.get(cameraId);
		return prefs.shutterDelay;
	}

	public void setShutterDelay(String cameraId, double value)
	{
		cameraPrefs prefs = cameras.get(cameraId);
		String key = prefs.getKey(CameraKeysEnum.SHUTTER_DELAY_KEY);

		prefs.shutterDelay = value;

		preferences.edit().putFloat(key, (float) value).apply();
	}

	public float getLocalZoom(String cameraId)
	{
		cameraPrefs prefs = cameras.get(cameraId);
		return prefs.localZoom;
	}

	public void setLocalZoom(String cameraId, float value)
	{
		cameraPrefs prefs = cameras.get(cameraId);
		String key = prefs.getKey(CameraKeysEnum.LOCAL_ZOOM_KEY);

		prefs.localZoom = value;

		preferences.edit().putFloat(key, (float) value).apply();
	}

	public float getRemoteZoom(String cameraId)
	{
		cameraPrefs prefs = cameras.get(cameraId);
		return prefs.remoteZoom;
	}

	public void setRemoteZoom(String cameraId, float value)
	{
		cameraPrefs prefs = cameras.get(cameraId);
		String key = prefs.getKey(CameraKeysEnum.REMOTE_ZOOM_KEY);

		prefs.remoteZoom = value;

		preferences.edit().putFloat(key, (float) value).apply();
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

	public boolean getFirstTime()
	{
		return firstTime;
	}

	public void setFirstTime(boolean first)
	{
		firstTime = first;
		preferences.edit().putBoolean(FIRST_TIME, firstTime).apply();
	}

	public String getId()
	{
		return id;
	}

	public Set<PhotoProcessor.CompositeImageType> getProcessorTypes()
	{
		return processorTypes;
	}

	public PreviewOverlayWidget.Type getOverlay()
	{
		return overlay;
	}

	public PreviewWidget.SHUTTER_TYPE getResolution()
	{
		return resolution;
	}
}

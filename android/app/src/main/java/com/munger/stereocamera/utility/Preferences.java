package com.munger.stereocamera.utility;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public class Preferences
{
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

	private boolean firstTime = true;
	private Set<PhotoProcessor.CompositeImageType> processorTypes;
	private PreviewOverlayWidget.Type overlay;
	private PreviewWidget.ShutterType resolution;
	private String id = null;

	public Preferences()
	{
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
			resolution = PreviewWidget.ShutterType.PREVIEW;
		else
			resolution = PreviewWidget.ShutterType.HI_RES;
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

	public PreviewWidget.ShutterType getResolution()
	{
		return resolution;
	}
}

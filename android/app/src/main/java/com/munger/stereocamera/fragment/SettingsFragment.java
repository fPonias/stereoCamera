package com.munger.stereocamera.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;

/**
 * Created by hallmarklabs on 3/19/18.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener
{
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
	{
		setPreferencesFromResource(R.xml.preferences, rootKey);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onStop() {
		super.onStop();

		PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance()).unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		//MyApplication.getInstance().getPrefs().updateSettingsPrefs();
	}
}

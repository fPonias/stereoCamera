package com.munger.stereocamera.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.munger.stereocamera.R;

/**
 * Created by hallmarklabs on 3/19/18.
 */

public class SettingsFragment extends PreferenceFragmentCompat
{
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
	{
		setPreferencesFromResource(R.xml.preferences, rootKey);
	}
}

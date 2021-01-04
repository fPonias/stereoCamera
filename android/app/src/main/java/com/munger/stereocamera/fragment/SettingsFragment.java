package com.munger.stereocamera.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import androidx.preference.PreferenceFragmentCompat;import com.munger.stereocamera.R;

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

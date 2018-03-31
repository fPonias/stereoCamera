package com.munger.stereocamera.utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

/**
 * Created by hallmarklabs on 3/27/18.
 */

public class InteractiveReceiver extends BroadcastReceiver
{
	private boolean screenOn = true;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_SCREEN_OFF))
			screenOn = false;
		else if (action.equals(Intent.ACTION_SCREEN_ON))
			screenOn = true;

		for (Listener listener : listeners)
			listener.screenChanged(screenOn);
	}

	public interface Listener
	{
		void screenChanged(boolean isOn);
	}

	private ArrayList<Listener> listeners = new ArrayList<>();

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}
}

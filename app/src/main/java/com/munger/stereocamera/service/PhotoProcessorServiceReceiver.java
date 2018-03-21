package com.munger.stereocamera.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;

/**
 * Created by hallmarklabs on 3/19/18.
 */

public class PhotoProcessorServiceReceiver extends BroadcastReceiver
{
	private PhotoFiles photoFiles = null;
	private Listener listener;
	private Context context;

	public PhotoProcessorServiceReceiver(Listener listener)
	{
		this.listener = listener;
	}

	public interface Listener
	{
		void onPhoto(String path);
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		this.context = context;

		if (photoFiles == null)
		{
			photoFiles = new PhotoFiles(context);
		}

		String action = intent.getAction();

		if (action.equals(PhotoProcessorService.BROADCAST_PROCESSED_ACTION))
		{
			String path = intent.getStringExtra(PhotoProcessorService.EXTENDED_DATA_PATH);
			listener.onPhoto(path);

		}
	}
}

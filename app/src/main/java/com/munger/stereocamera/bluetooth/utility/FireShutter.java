package com.munger.stereocamera.bluetooth.utility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.util.ArraySet;
import android.support.v7.preference.PreferenceManager;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Shutter;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.utility.PhotoFiles;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class FireShutter extends FireShutterAbstract
{
	public FireShutter(MasterFragment fragment)
	{
		super(fragment);
	}

	public void execute(final long delay, final Listener listener)
	{
		synchronized (lock)
		{
			if (shutterFiring)
				return;

			this.listener = listener;
			shutterFiring = true;
		}

		fireLocal(delay);
		fireRemote();
	}

	protected void fireRemote()
	{
		masterComm.runCommand(new Shutter(), new BluetoothMasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				Shutter.Response r = (Shutter.Response) response;
				handleRemote(r.data, r.orientation, r.zoom);
			}

			@Override
			public void onFail()
			{
				remoteData = null;
				boolean doNext = false;

				synchronized (lock)
				{
					remoteShutterDone = true;

					if (localShutterDone)
						doNext = true;
				}

				if (doNext)
					done();
			}
		});
	}
}

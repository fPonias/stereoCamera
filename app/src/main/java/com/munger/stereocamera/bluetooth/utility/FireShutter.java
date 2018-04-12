package com.munger.stereocamera.bluetooth.utility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.util.ArraySet;
import android.support.v7.preference.PreferenceManager;

import com.munger.stereocamera.MainActivity;
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

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private PhotoProcessorService.PhotoArgument localData;
	private PhotoProcessorService.PhotoArgument remoteData;

	private BluetoothMasterComm masterComm;
	private MasterFragment fragment;

	public FireShutter(MasterFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MainActivity.getInstance().getBtCtrl().getMaster().getComm();
		photoFiles = new PhotoFiles(fragment.getContext());
	}

	public interface Listener
	{
		void onProcessing();
		void done();
		void fail();
	}

	private Listener listener;

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

	private void fireLocal(final long wait)
	{
		localData = new PhotoProcessorService.PhotoArgument();

		Thread t = new Thread(new Runnable() {public void run()
		{
			try { Thread.sleep(wait); } catch(InterruptedException e){}

			fragment.fireShutter(new PreviewFragment.ImageListener()
			{
				@Override
				public void onImage(byte[] bytes)
				{
					handleLocal(bytes);
				}

				@Override
				public void onFinished()
				{}
			});
			localData.orientation = MainActivity.getInstance().getCurrentOrientation();
			localData.zoom = fragment.getZoom();

			MainActivity.getInstance().getPrefs().setLocalZoom(fragment.getCameraId(), fragment.getZoom());
		}});
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	private void handleLocal(byte[] bytes)
	{
		String localPath = photoFiles.saveDataToCache(bytes);
		localData.jpegPath = localPath;

		boolean doNext = false;

		synchronized (lock)
		{
			localShutterDone = true;

			if (remoteShutterDone)
				doNext = true;

			shutterFiring = false;
			lock.notify();
		}

		if (doNext)
			done();
	}

	private void fireRemote()
	{
		masterComm.runCommand(new Shutter(), new BluetoothMasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				Shutter.Response r = (Shutter.Response) response;
				remoteData = new PhotoProcessorService.PhotoArgument();
				remoteData.orientation = r.orientation;
				remoteData.zoom = r.zoom;

				String remotePath = photoFiles.saveDataToCache(r.data);
				remoteData.jpegPath = remotePath;

				MainActivity.getInstance().getPrefs().setRemoteZoom(fragment.getCameraId(), r.zoom);

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

	private PhotoFiles photoFiles;

	private void done()
	{
		listener.onProcessing();
		photoFiles = new PhotoFiles(fragment.getContext());
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				if (localData == null || remoteData == null)
				{
					listener.fail();
					return;
				}

				boolean isOnLeft = MainActivity.getInstance().getPrefs().getIsOnLeft();

				if (!isOnLeft)
				{
					PhotoProcessorService.PhotoArgument tmp = remoteData;
					remoteData = localData;
					localData = tmp;
				}

				Context c = MainActivity.getInstance();
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);

				ArraySet<String> defaults = new ArraySet<>();
				defaults.add("split");
				Set<String> formats = sharedPref.getStringSet("pref_formats", defaults);

				Iterator<String> iter = formats.iterator();
				while(iter.hasNext())
				{
					String key = iter.next();
					PhotoProcessor.CompositeImageType type;
					switch(key)
					{
						case "split":
						default:
							type = PhotoProcessor.CompositeImageType.SPLIT;
							break;
						case "red blue":
							type = PhotoProcessor.CompositeImageType.RED_CYAN;
							break;
						case "green mag":
							type = PhotoProcessor.CompositeImageType.GREEN_MAGENTA;
							break;
					}

					Intent i = PhotoProcessorService.getIntent(localData, remoteData, fragment.getFacing(), type);
					MainActivity.getInstance().startService(i);
				}

				listener.done();
			}

			@Override
			public void fail()
			{
				final AlertDialog dialog = new AlertDialog.Builder(fragment.getContext())
						.setMessage(R.string.thumbnail_filesystem_error)
						.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							dialogInterface.dismiss();
						}})
						.create();
				dialog.show();
			}
		});
	}
}

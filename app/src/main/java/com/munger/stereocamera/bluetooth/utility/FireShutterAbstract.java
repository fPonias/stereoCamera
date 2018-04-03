package com.munger.stereocamera.bluetooth.utility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.util.ArraySet;
import android.support.v7.preference.PreferenceManager;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.bluetooth.command.master.commands.Shutter;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.utility.PhotoFiles;

import java.util.Iterator;
import java.util.Set;

public class FireShutterAbstract
{
	protected final Object lock = new Object();
	protected boolean localShutterDone = false;
	protected boolean remoteShutterDone = false;
	protected boolean shutterFiring = false;

	protected PhotoProcessorService.PhotoArgument localData;
	protected PhotoProcessorService.PhotoArgument remoteData;

	protected BluetoothMasterComm masterComm;
	protected MasterFragment fragment;

	protected Listener listener;

	public interface Listener
	{
		void onProcessing();
		void done();
		void fail();
	}

	public FireShutterAbstract(MasterFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
		photoFiles = new PhotoFiles(fragment.getContext());
	}

	protected void fireLocal(final long wait)
	{
		localData = new PhotoProcessorService.PhotoArgument();

		Thread t = new Thread(new Runnable() {public void run()
		{
			if (wait > 0)
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
			localData.orientation = fragment.getCurrentOrientation();
			localData.zoom = fragment.getZoom();

			MyApplication.getInstance().getPrefs().setLocalZoom(fragment.getCameraId(), fragment.getZoom());
		}});
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	protected void handleLocal(byte[] bytes)
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

	protected void handleRemote(byte[] data, PhotoOrientation orientation, float zoom)
	{
		remoteData = new PhotoProcessorService.PhotoArgument();
		remoteData.orientation = orientation;
		remoteData.zoom = zoom;

		String remotePath = photoFiles.saveDataToCache(data);
		remoteData.jpegPath = remotePath;

		MyApplication.getInstance().getPrefs().setRemoteZoom(fragment.getCameraId(), zoom);

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

	protected PhotoFiles photoFiles;

	protected void done()
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
					onFail();
					return;
				}

				boolean isOnLeft = MyApplication.getInstance().getPrefs().getIsOnLeft();

				if (!isOnLeft)
				{
					PhotoProcessorService.PhotoArgument tmp = remoteData;
					remoteData = localData;
					localData = tmp;
				}

				Context c = MyApplication.getInstance();
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
					MyApplication.getInstance().startService(i);
				}

				onDone();
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

	protected void onDone()
	{
		listener.done();
	}

	protected void onFail()
	{
		listener.fail();
	}
}

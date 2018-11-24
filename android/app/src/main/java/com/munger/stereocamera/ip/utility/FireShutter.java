package com.munger.stereocamera.ip.utility;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.util.ArraySet;
import android.support.v7.preference.PreferenceManager;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.ip.command.Comm;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.commands.SendPhoto;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.service.PhotoProcessorExec;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.R;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.widget.PreviewWidget;

import java.io.File;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private PhotoProcessorService.PhotoArgument localData;
	private PhotoProcessorService.PhotoArgument remoteData;
	private boolean flip = false;

	private CommCtrl masterComm;
	private MasterFragment fragment;

	public FireShutter(MasterFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MainActivity.getInstance().getCtrl();
		photoFiles = new PhotoFiles(fragment.getContext());
	}

	public interface Listener
	{
		void onProcessing();
		void done();
		void fail();
	}

	private Listener listener;

	public void execute(final long delay, PreviewWidget.SHUTTER_TYPE type, final Listener listener)
	{
		synchronized (lock)
		{
			if (shutterFiring)
				return;

			this.listener = listener;
			shutterFiring = true;
		}

		fireLocal(type, delay);
		fireRemote(type);
	}

	private void fireLocal(final PreviewWidget.SHUTTER_TYPE type, final long wait)
	{
		synchronized (lock)
		{
			localData = new PhotoProcessorService.PhotoArgument();
		}

		Thread t = new Thread(new Runnable() {public void run()
		{
			try { Thread.sleep(wait); } catch(InterruptedException e){}

			fragment.fireShutter(type, new PreviewWidget.ImageListener()
			{
				@Override
				public void onImage(byte[] bytes)
				{
					synchronized (lock)
					{
						String localPath = photoFiles.saveDataToCache(bytes);
						localData.jpegPath = localPath;
					}

					handleLocal();
				}

				@Override
				public void onFinished()
				{}
			});

			synchronized (lock)
			{
				localData.orientation = MainActivity.getInstance().getCurrentOrientation();
				localData.zoom = fragment.getZoom();
			}

			handleLocal();

			MainActivity.getInstance().getPrefs().setLocalZoom(fragment.getCameraId(), fragment.getZoom());
		}});
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	private void handleLocal()
	{
		boolean doNext = false;

		synchronized (lock)
		{
			if (localData.jpegPath == null || localData.orientation == null)
				return;

			localShutterDone = true;

			if (remoteShutterDone)
				doNext = true;

			shutterFiring = false;
			lock.notify();
		}

		if (doNext)
			done();
	}

	private void fireRemote(PreviewWidget.SHUTTER_TYPE type)
	{
		masterComm.sendCommand(new com.munger.stereocamera.ip.command.commands.FireShutter(), new CommCtrl.ResponseListener()
		{
			@Override
			public void onReceived(Command command, Command origCommand)
			{
				com.munger.stereocamera.ip.command.commands.FireShutter r = (com.munger.stereocamera.ip.command.commands.FireShutter) command;
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
			public void onReceiveFailed(Command command)
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
		}, 30000);
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

				done2();
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

	private void done2()
	{
		//PhotoProcessor proc = new PhotoProcessor(this, type);
		PhotoProcessorExec proc = new PhotoProcessorExec(MainActivity.getInstance(), PhotoProcessorExec.CompositeImageType.SPLIT);

		proc.setData(false, localData.jpegPath, localData.orientation, localData.zoom);
		proc.setData(true, remoteData.jpegPath, remoteData.orientation, remoteData.zoom);

		String out = proc.processData(flip);

		if (out == null)
		{
			listener.fail();
			return;
		}

		handleProcessedPhoto(out);
	}

	public void handleProcessedPhoto(final String path)
	{
		if (photoFiles == null)
			photoFiles = new PhotoFiles(MainActivity.getInstance());

		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				handlePhotoProcessed2(path);
			}

			@Override
			public void fail()
			{
				listener.fail();
			}
		});
	}

	private void handlePhotoProcessed2(String path)
	{
		File fl = new File(path);
		String newPath = photoFiles.saveNewFile(fl);

		masterComm.sendCommand(new SendPhoto(newPath), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() {public void r(boolean success, Command command, Command originalCmd)
		{
			if (success)
				listener.done();
			else
				listener.fail();
		}}), 30000);

		MainActivity.getInstance().onNewPhoto(newPath);
	}
}

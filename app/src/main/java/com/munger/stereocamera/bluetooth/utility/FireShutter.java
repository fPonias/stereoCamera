package com.munger.stereocamera.bluetooth.utility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Shutter;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.utility.PhotoFiles;

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
		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
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
			localData.orientation = fragment.getCurrentOrientation();
			localData.zoom = fragment.getZoom();
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
		masterComm.runCommand(new Shutter(new Shutter.Listener()
		{
			@Override
			public void onData(PhotoOrientation orientation, float zoom, byte[] data)
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

			@Override
			public void fail()
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
		}));
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

				boolean isOnLeft = MyApplication.getInstance().getPrefs().getIsOnLeft();

				if (!isOnLeft)
				{
					PhotoProcessorService.PhotoArgument tmp = remoteData;
					remoteData = localData;
					localData = tmp;
				}

				Intent i = new Intent(MyApplication.getInstance(), PhotoProcessorService.class);
				i.putExtra(PhotoProcessorService.FLIP_ARG, fragment.getFacing());
				i.putExtra(PhotoProcessorService.LEFT_PHOTO_ARG, localData);
				i.putExtra(PhotoProcessorService.RIGHT_PHOTO_ARG, remoteData);

				MyApplication.getInstance().startService(i);

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

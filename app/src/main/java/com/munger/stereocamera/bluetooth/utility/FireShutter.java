package com.munger.stereocamera.bluetooth.utility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Shutter;
import com.munger.stereocamera.fragment.PreviewFragment;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.PhotoProcessor;

import java.io.ByteArrayOutputStream;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private PhotoStruct localData;
	private PhotoStruct remoteData;

	private BluetoothMasterComm masterComm;
	private PreviewFragment fragment;

	public FireShutter(PreviewFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
	}

	private static class PhotoStruct
	{
		public byte[] data;
		public PhotoOrientation orientation;
		public float zoom;
	}

	public interface Listener
	{
		void onProcessing();
		void done(String path);
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
		localData = new PhotoStruct();

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
			localData.zoom = fragment.getZoomValue();
		}});
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
	}

	private void handleLocal(byte[] bytes)
	{
		localData.data = bytes;

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
				remoteData = new PhotoStruct();
				remoteData.orientation = orientation;
				remoteData.zoom = zoom;
				remoteData.data	= data;

				MyApplication.getInstance().getPrefs().setRemoteZoom(zoom);

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
		photoFiles = new PhotoFiles();
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
					PhotoStruct tmp = remoteData;
					remoteData = localData;
					localData = tmp;
				}

				String name = "left.jpg";
				photoFiles.saveFile(name, localData.data);
				name = "right.jpg";
				photoFiles.saveFile(name, remoteData.data);

				PhotoProcessor proc = new PhotoProcessor(fragment.getContext());
				//byte[] merged = proc.processData(localData, remoteData, fragment.getFacing());
				//name = photoFiles.saveNewFile(merged);

				listener.done(photoFiles.getFilePath(name));
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

	private void BitmapToFile(Bitmap out, String name)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();

		photoFiles.saveFile(name, ret);
	}

}

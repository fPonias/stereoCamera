package com.munger.stereocamera.bluetooth.utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.Shutter;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.ByteArrayOutputStream;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private byte[] localData;
	private byte[] remoteData;

	private BluetoothMasterComm masterComm;
	private PreviewFragment fragment;

	public FireShutter(PreviewFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
	}

	public void execute(final long localLatency, final long remoteLatency)
	{
		synchronized (lock)
		{
			if (shutterFiring)
				return;

			shutterFiring = true;
		}

		long diff = remoteLatency - localLatency;
		fireLocal(diff);
		fireRemote();
	}

	private void fireLocal(final long wait)
	{
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
			});
		}});
		t.start();
	}

	private void handleLocal(byte[] bytes)
	{
		localData = bytes;
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
			public void onData(byte[] data)
			{
				remoteData = data;
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

	private void done()
	{
		final PhotoFiles photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				byte[] merged = processData(localData, remoteData, fragment.isFrontFacing());
				String name = photoFiles.saveNewFile(merged);

				//localName = max + ".left.jpg";
				//photoFiles.saveFile(localName, localData);
				//localName = max + ".right.jpg";
				//photoFiles.saveFile(localName, remoteData);
			}
		});
	}

	private byte[] processData(byte[] left, byte[] right, boolean flip)
	{
		Bitmap leftBmp = BitmapFactory.decodeByteArray(left, 0, left.length);
		Bitmap rightBmp = BitmapFactory.decodeByteArray(right, 0, right.length);

		if (flip)
		{
			Bitmap tmp = rightBmp;
			rightBmp = leftBmp;
			leftBmp = tmp;
		}

		int h1 = leftBmp.getHeight();
		int w1 = leftBmp.getWidth();
		int h2 = rightBmp.getHeight();
		int w2 = rightBmp.getWidth();

		int w = (h1 > h2) ? h1 : h2;

		int degrees = 90;
		if (flip)
			degrees = -degrees;

		Matrix m1 = new Matrix();

		if (h1 < h2)
		{
			float scale = (float) h2 / (float) h1;
			m1.postScale(scale, scale);
		}

		m1.postRotate(degrees);
		Bitmap leftRotated = Bitmap.createBitmap(leftBmp, 0, 0, h1, h1, m1, true);

		Matrix m2 = new Matrix();

		if (h2 < h1)
		{
			float scale = (float) h1 / (float) h2;
			m2.postScale(scale, scale);
		}

		m2.postRotate(degrees);
		Bitmap rightRotated = Bitmap.createBitmap(rightBmp, 0, 0, h2, h2, m2, true);

		Bitmap out = Bitmap.createBitmap(w * 2, w, leftBmp.getConfig());

		int[] buffer = new int[w * w];
		leftRotated.getPixels(buffer, 0, w, 0, 0, w, w);
		out.setPixels(buffer, 0, w, 0, 0, w, w);
		leftRotated.recycle();

		rightRotated.getPixels(buffer, 0, w, 0, 0, w, w);
		out.setPixels(buffer, 0, w, w, 0, w, w);
		rightRotated.recycle();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();
		return ret;
	}
}

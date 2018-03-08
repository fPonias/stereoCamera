package com.munger.stereocamera.bluetooth.utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.PhotoOrientations;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Shutter;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.ByteArrayOutputStream;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private static class PhotoStruct
	{
		public byte[] data;
		public PhotoOrientations orientation;
		public float zoom;
	}

	private PhotoStruct localData;
	private PhotoStruct remoteData;

	private BluetoothMasterComm masterComm;
	private PreviewFragment fragment;

	public FireShutter(PreviewFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
	}

	public void execute(final long delay)
	{
		synchronized (lock)
		{
			if (shutterFiring)
				return;

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
			public void onData(PhotoOrientations orientation, float zoom, byte[] data)
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
		photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				if (localData == null || remoteData == null)
					return;

				boolean isOnLeft = MyApplication.getInstance().getPrefs().getIsOnLeft();

				if (!isOnLeft)
				{
					PhotoStruct tmp = remoteData;
					remoteData = localData;
					localData = tmp;
				}

				byte[] merged = processData(localData, remoteData, fragment.isFrontFacing());
				String name = photoFiles.saveNewFile(merged);

				//localName = max + ".left.jpg";
				//photoFiles.saveFile(localName, localData);
				//localName = max + ".right.jpg";
				//photoFiles.saveFile(localName, remoteData);
			}
		});
	}

	private byte[] processData(PhotoStruct left, PhotoStruct right, boolean flip)
	{
		if (flip)
		{
			PhotoStruct tmp = right;
			right = left;
			left = tmp;
		}

		Bitmap leftBmp = BitmapFactory.decodeByteArray(left.data, 0, left.data.length);
		BitmapToFile(leftBmp, "left-1.jpg");
		Bitmap rightBmp = BitmapFactory.decodeByteArray(right.data, 0, right.data.length);
		int leftWidth = Math.min(leftBmp.getHeight(), leftBmp.getWidth());
		int leftHeight = Math.max(leftBmp.getHeight(), leftBmp.getWidth());
		int rightWidth = Math.min(rightBmp.getHeight(), rightBmp.getWidth());
		int rightHeight = Math.max(rightBmp.getHeight(), rightBmp.getWidth());
		int targetDim = Math.max(leftWidth, rightWidth);
		float leftScale = 1.0f;
		float rightScale = 1.0f;

		if (leftWidth < rightWidth)
			leftScale = (float) rightWidth / (float) leftWidth;
		else if (leftWidth > rightWidth)
			rightScale = (float) leftWidth / (float) rightWidth;

		double leftRotate = -left.orientation.toDegress() + 90.0f;
		double rightRotate = -right.orientation.toDegress() + 90.0f;
		if (flip)
		{
			leftRotate = -leftRotate;
			rightRotate = -rightRotate;
		}

		Matrix m1 = new Matrix();
		m1.postRotate((float) leftRotate);
		m1.postScale(leftScale, leftScale);
		leftBmp = Bitmap.createBitmap(leftBmp, 0, 0, leftBmp.getWidth(), leftBmp.getHeight(), m1, true);
		BitmapToFile(leftBmp, "left-1-manip.jpg");

		m1 = new Matrix();
		m1.postRotate((float) rightRotate);
		m1.postScale(rightScale, rightScale);
		rightBmp = Bitmap.createBitmap(rightBmp, 0, 0, rightBmp.getWidth(), rightBmp.getHeight(), m1, true);


		Bitmap out = Bitmap.createBitmap(targetDim * 2, targetDim, leftBmp.getConfig());

		int[] buffer = new int[targetDim * targetDim];
		leftBmp.getPixels(buffer, 0, targetDim, 0, 0, targetDim, targetDim);
		out.setPixels(buffer, 0, targetDim, 0, 0, targetDim, targetDim);
		leftBmp.recycle();

		rightBmp.getPixels(buffer, 0, targetDim, 0, 0, targetDim, targetDim);
		out.setPixels(buffer, 0, targetDim, targetDim, 0, targetDim, targetDim);
		rightBmp.recycle();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();
		return ret;
	}

	private void BitmapToFile(Bitmap out, String name)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();

		photoFiles.saveFile(name, ret);
	}
}

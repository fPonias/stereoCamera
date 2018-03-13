package com.munger.stereocamera.bluetooth.utility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Shutter;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private static class PhotoStruct
	{
		public byte[] data;
		public PhotoOrientation orientation;
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

				byte[] merged = processData(localData, remoteData, fragment.getFacing());
				name = photoFiles.saveNewFile(merged);

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

	public void testOldData()
	{
		photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				localData = new PhotoStruct();
				localData.data = photoFiles.loadFile("left.jpg");
				localData.orientation = PhotoOrientation.DEG_0;
				localData.zoom = 1.0f;
				remoteData = new PhotoStruct();
				remoteData.data = photoFiles.loadFile("right.jpg");
				remoteData.orientation = PhotoOrientation.DEG_0;
				remoteData.zoom = 1.27f;

				byte[] merged = processData(localData, remoteData, fragment.getFacing());
				String name = photoFiles.saveNewFile(merged);
			}

			@Override
			public void fail()
			{

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
		leftBmp = squareBitmap(left, leftBmp);
		leftBmp = rotateBitmap(left, leftBmp, flip);
		leftBmp = zoomBitmap(left, leftBmp);

		Bitmap rightBmp = BitmapFactory.decodeByteArray(right.data, 0, right.data.length);
		rightBmp = squareBitmap(right, rightBmp);
		rightBmp = rotateBitmap(right, rightBmp, flip);
		rightBmp = zoomBitmap(right, rightBmp);

		leftBmp = matchBitmap(leftBmp, rightBmp);
		Bitmap out = combineBitmaps(leftBmp, rightBmp);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();
		out.recycle();
		return ret;
	}

	private Bitmap squareBitmap(PhotoStruct data, Bitmap intermediate)
	{
		int w = intermediate.getWidth();
		int h = intermediate.getHeight();
		int target = Math.min(w, h);
		int margin = (Math.max(w, h) - target) / 2;

		Bitmap ret = Bitmap.createBitmap(intermediate, margin, 0, target, target);
		return ret;
	}

	private Bitmap rotateBitmap(PhotoStruct data, Bitmap intermediate, boolean flip)
	{
		double rotate = -data.orientation.toDegress() + 90.0f;
		if (flip)
			rotate = -rotate;
		int w = intermediate.getWidth();
		int h = intermediate.getHeight();

		Matrix m1 = new Matrix();
		m1.postRotate((float) rotate);
		Bitmap rot = Bitmap.createBitmap(intermediate, 0, 0, w, h, m1, true);
		intermediate.recycle();
		return rot;
	}

	private Bitmap zoomBitmap(PhotoStruct data, Bitmap intermediate)
	{
		if(data.zoom == 1.0f)
			return intermediate;

		int w = intermediate.getWidth();
		int h = intermediate.getHeight();
		float newW = w * data.zoom;
		float newH = h * data.zoom;
		float marginLeft = (newW  - (float) w) / 2.0f;
		float marginTop = (newH - (float) h) / 2.0f;

		Matrix m1 = new Matrix();
		m1.postScale(data.zoom, data.zoom);

		Bitmap scaled = Bitmap.createBitmap(intermediate, 0, 0, w, h, m1, true);
		Bitmap cropped = Bitmap.createBitmap(scaled, (int) marginLeft, (int) marginTop, w, h);
		intermediate.recycle();
		scaled.recycle();
		return cropped;
	}

	private Bitmap matchBitmap(Bitmap mutate, Bitmap source)
	{
		int target = source.getWidth();
		Bitmap ret = Bitmap.createScaledBitmap(mutate, target, target, false);
		mutate.recycle();
		return ret;
	}

	private Bitmap combineBitmaps(Bitmap leftBmp, Bitmap rightBmp)
	{
		int targetDim = leftBmp.getWidth();
		Bitmap out = Bitmap.createBitmap(targetDim * 2, targetDim, leftBmp.getConfig());

		int[] buffer = new int[targetDim * targetDim];
		leftBmp.getPixels(buffer, 0, targetDim, 0, 0, targetDim, targetDim);
		out.setPixels(buffer, 0, targetDim, 0, 0, targetDim, targetDim);

		rightBmp.getPixels(buffer, 0, targetDim, 0, 0, targetDim, targetDim);
		out.setPixels(buffer, 0, targetDim, targetDim, 0, targetDim, targetDim);

		leftBmp.recycle();
		rightBmp.recycle();

		return out;
	}

	private void BitmapToFile(Bitmap out, String name)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();

		photoFiles.saveFile(name, ret);
	}
}

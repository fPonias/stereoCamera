package com.munger.stereocamera.utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.utility.FireShutter;

import java.io.ByteArrayOutputStream;

/**
 * Created by hallmarklabs on 3/14/18.
 */

public class PhotoProcessor
{
	public static class PhotoStruct
	{
		public byte[] data;
		public PhotoOrientation orientation;
		public float zoom;
	}
	
	private PhotoFiles photoFiles;
	private PhotoStruct localData;
	private PhotoStruct remoteData;
	
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

				byte[] merged = processData(localData, remoteData, true);
				String name = photoFiles.saveNewFile(merged);
			}

			@Override
			public void fail()
			{

			}
		});
	}

	public byte[] processData(PhotoStruct left, PhotoStruct right, boolean flip)
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
}

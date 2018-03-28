package com.munger.stereocamera.service;

import android.content.Context;
import android.content.Intent;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;

public class PhotoProcessor
{
	static
	{
		System.loadLibrary("native-lib");
	}

	private PhotoFiles photoFiles;
	private Context context;

	public PhotoProcessor(Context context, CompositeImageType type)
	{
		this.context = context;
		photoFiles = new PhotoFiles(context);
		initN(context.getCacheDir().getPath());
		setProcessorType(type.ordinal());
	}

	public void testOldData()
	{
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				Thread t = new Thread(new Runnable() { public void run()
				{
					PhotoProcessorService.PhotoArgument localData = new PhotoProcessorService.PhotoArgument();
					localData.jpegPath = "/data/local/tmp/left.jpg";
					localData.orientation = PhotoOrientation.DEG_90;
					localData.zoom = 1.5f;

					PhotoProcessorService.PhotoArgument remoteData = new PhotoProcessorService.PhotoArgument();
					remoteData.jpegPath = "/data/local/tmp/left.jpg";
					remoteData.orientation = PhotoOrientation.DEG_180;
					remoteData.zoom = 1.0f;

					Intent i = PhotoProcessorService.getIntent(localData, remoteData, false, CompositeImageType.SPLIT);
					MyApplication.getInstance().startService(i);
				}});
				t.start();
			}

			@Override
			public void fail()
			{

			}
		});
	}

	public void setData(boolean isRight, byte[] data, PhotoOrientation orientation, float zoom)
	{
		String path = photoFiles.saveDataToCache(data);
		setImageN(isRight, path, orientation.ordinal(), zoom);
	}

	public void setData(boolean isRight, String path, PhotoOrientation orientation, float zoom)
	{
		setImageN(isRight, path, orientation.ordinal(), zoom);
	}

	public String processData(boolean flip)
	{
		File out = photoFiles.getRandomFile();
		processN(true, flip, out.getPath());

		if (!out.exists() || out.length() == 0)
		{
			cleanUpN();
			return null;
		}

		cleanUpN();

		return out.getPath();
	}

	/**
	 * init the native engine
	 * @param cachePath folder to store temporary data
	 */
	private native void initN(String cachePath);

	//mirror enum of data type in CompositeImage.h
	public enum CompositeImageType
	{
		SPLIT,
		GREEN_MAGENTA,
		RED_BLUE
	};

	/**
	 * set the type of image to produce
	 * @param type ordinal value of CompositeImageType
	 */
	private native void setProcessorType(int type);

	/**
	 * Send raw data to the native code
	 * @param isRight left or right?
	 * @param path path to pixel data
	 * @param orientation number of times to rotation the image clockwise by 90 degrees
	 * @param zoom digital "zoom" factor starting at 1.0f
	 */
	private native void setImageN(boolean isRight, String path, int orientation, float zoom);

	/**
	 * process all data and combine to a composite image without crashing the phone due to memory errors
	 * @param growToMaxDim true the smaller resolution side will be grown to match, otherwise shrink it
	 * @param flip was the image taken with the rear or front facing camera?
	 * @param targetPath the path to save the final image to
	 */
	private native void processN(boolean growToMaxDim, boolean flip, String targetPath);

	/**
	 * free any leftover malloced data or java references
	 */
	private native void cleanUpN();
}

package com.munger.stereocamera.service;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoProcessor
{
	static
	{
		System.loadLibrary("native-lib");
	}

	private PhotoFiles photoFiles;
	private Context context;

	public PhotoProcessor(Context context)
	{
		this.context = context;
		photoFiles = PhotoFiles.Factory.get();
		initN(context.getCacheDir().getPath());
		setProcessorType(PhotoProcessor.CompositeImageType.SPLIT.ordinal());
	}

	public void testOldData()
	{
	}

	public static void copy(File src, File dst) throws IOException
	{
		if  (src.getPath().equals(dst.getPath()))
			return;

		InputStream in = new FileInputStream(src);
		try
		{
			OutputStream out = new FileOutputStream(dst);
			try
			{
				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0)
				{
					out.write(buf, 0, len);
				}
			}
			catch(Exception e){

			}
			finally {
				out.close();
			}
		}
		catch(Exception e){

		}
		finally {
			in.close();
		}
	}

	//make sure this is called before setting image data as native code cleans the image data
	public void setProcessorType(CompositeImageType type)
	{
		setProcessorType(type.ordinal());
	}

	public void setData(boolean isRight, byte[] data, PhotoOrientation orientation, float zoom)
	{
		String path = photoFiles.saveDataToCache(data);
		setImageN(isRight, path, orientation.ordinal(), zoom);
	}

	public void setData(boolean isRight, ImagePair.ImageArg arg)
	{
		setImageN(isRight, arg.path, arg.orientation.ordinal(), arg.zoom);
	}

	public void preProcess(boolean flip)
	{
		preProcessN(flip);
	}

	public String processData()
	{
		File out = photoFiles.getRandomFile();
		processN(true, out.getPath());

		return out.getPath();
	}

	public void clean()
	{
		cleanUpN();
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
		RED_CYAN
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
	 * crop and rotate new images set with setImageN
	 * @param flip was the image taken with the rear or front facing camera?
	 */
	private native void preProcessN(boolean flip);

	/**
	 * process all data and combine to a composite image without crashing the phone due to memory errors
	 * @param growToMaxDim true the smaller resolution side will be grown to match, otherwise shrink it
	 * @param targetPath the path to save the final image to
	 */
	private native void processN(boolean growToMaxDim, String targetPath);

	/**
	 * free any leftover malloced data or java references
	 */
	private native void cleanUpN();
}

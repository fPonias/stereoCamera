package com.munger.stereocamera.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.provider.ContactsContract;

import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.utility.FireShutter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;


/**
 * Created by hallmarklabs on 3/14/18.
 */

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
		initN(context.getCacheDir().getPath());
	}

	public void testOldData()
	{
		photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				//String path = photoFiles.getFilePath("left.jpg");
				String path = "/data/local/tmp/left.jpg";
				setImageN(false, path, PhotoOrientation.DEG_0.ordinal(), 1.0f);
				//path = photoFiles.getFilePath("right.jpg");
				path = "/data/local/tmp/left.jpg";
				setImageN(true, path, PhotoOrientation.DEG_0.ordinal(), 1.0f);

				String result = processData(false);
			}

			@Override
			public void fail()
			{

			}
		});
	}

	public void setData(boolean isRight, byte[] data, PhotoOrientation orientation, float zoom)
	{
		String path = saveDataToCache(data);
		setImageN(isRight, path, orientation.ordinal(), zoom);
	}

	private String saveDataToCache(byte[] data)
	{
		String outputPath = null;
		FileOutputStream fos = null;

		try
		{
			File out = getRandomFile();
			fos = new FileOutputStream(out);

			fos.write(data);

			fos.flush();
			outputPath = out.getPath();
		}
		catch(IOException e){

		}
		finally{
			try
			{
				if (fos != null)
					fos.close();
			}
			catch(IOException e){}
		}

		return outputPath;
	}

	private File getRandomFile()
	{
		File f = context.getCacheDir();
		File out = null;
		String path = f.getPath() + "/";

		do
		{
			int id = (int) ((double) Integer.MAX_VALUE * Math.random());
			out = new File(path + id);
		} while (out.exists());

		return out;
	}

	public String processData(boolean flip)
	{
		File out = getRandomFile();
		processN(true, flip, out.getPath());

		if (!out.exists() || out.length() == 0)
		{
			cleanUpN();
			return null;
		}

		String savedPath = photoFiles.saveNewFile(out);
		cleanUpN();

		return savedPath;
	}

	/**
	 * init the native engine
	 * @param cachePath folder to store temporary data
	 */
	private native void initN(String cachePath);

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

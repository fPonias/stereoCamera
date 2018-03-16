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
				byte[] data = photoFiles.loadFile("left.jpg");
				setData(false, data, PhotoOrientation.DEG_0, 1.0f);
				data = photoFiles.loadFile("right.jpg");
				setData(true, data, PhotoOrientation.DEG_0, 1.0f);

				Bitmap merged = processData(false);

				String name = photoFiles.saveNewBitmap(merged);
			}

			@Override
			public void fail()
			{

			}
		});
	}

	public void setData(boolean isRight, byte[] data, PhotoOrientation orientation, float zoom)
	{
		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
		String path = saveBitmapToCache(bmp, orientation, zoom);
		setImageN(isRight, path, orientation.ordinal(), zoom, bmp.getWidth(), bmp.getHeight());
		bmp.recycle();

	}

	private String saveBitmapToCache(Bitmap bmp, PhotoOrientation orientation, float zoom)
	{
		String outputPath = null;
		FileOutputStream fos = null;

		try
		{
			File out = getRandomFile();
			fos = new FileOutputStream(out);
			writeBitmap(fos, bmp);

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

	private void writeBitmap(FileOutputStream fos, Bitmap bmp) throws IOException
	{
		int w = bmp.getWidth();
		int h = bmp.getHeight();
		int[] buffer = new int[w];
		ByteBuffer bb = ByteBuffer.allocate(w * 4);

		for (int y = 0; y < h; y++)
		{
			bmp.getPixels(buffer, 0, w, 0, y, w, 1);

			bb.rewind();
			IntBuffer ibuf = bb.asIntBuffer();
			ibuf.put(buffer);

			fos.write(bb.array());
		}
	}

	public Bitmap processData(boolean flip)
	{
		processN(true, flip);
		int sz = getProcessedDimension();
		String path = getProcessedPathN();
		Bitmap ret = readBitmap(path, sz);

		cleanUpN();

		return ret;
	}

	private Bitmap readBitmap(String path, int dim)
	{
		int totalSz = dim * 2 * dim;
		int[] data = new int[totalSz];

		FileInputStream fis = null;
		try
		{
			File f = new File(path);
			fis = new FileInputStream(f);

			byte[] buffer = new byte[dim * 2 * 4];
			ByteBuffer bb = ByteBuffer.wrap(buffer);
			int total = 0;
			int read = 1;
			while (read > 0 && total < totalSz)
			{
				read = fis.read(buffer);
				int iRead = read / 4;
				for (int i = 0; i < iRead; i++)
					data[total + i] = bb.getInt(i * 4);

				total += iRead;
			}
		}
		catch(IOException e){
			int i = 0;
			int j = i;
		}
		finally{
			try
			{
				if (fis != null)
					fis.close();
			}
			catch(IOException e) {}
		}

		Bitmap bmp = Bitmap.createBitmap(data, dim * 2, dim, Bitmap.Config.ARGB_8888);
		return bmp;
	}

	/**
	 * initialize native code to write temp data to the cachePath and create temporary variables needed for processing.
	 * @param cachePath
	 */
	private native void initN(String cachePath);

	/**
	 * Send raw data to the native code
	 * @param isRight left or right?
	 * @param path path to pixel data
	 * @param orientation number of times to rotation the image clockwise by 90 degrees
	 * @param zoom digital "zoom" factor starting at 1.0f
	 * @param width the width of the bitmap
	 * @param height the height of the bitmap
	 */
	private native void setImageN(boolean isRight, String path, int orientation, float zoom, int width, int height);

	/**
	 * process all data and combine to a composite image without crashing the phone due to memory errors
	 * @param growToMaxDim true the smaller resolution side will be grown to match, otherwise shrink it
	 * @param flip was the image taken with the rear or front facing camera?
	 */
	private native void processN(boolean growToMaxDim, boolean flip);

	/**
	 * get the path to the final processed bitmap data
	 * @return
	 */
	private native String getProcessedPathN();

	/**
	 * get the final dimension of the processed data.  Returned value is the height of the image and half the width.
	 * @return
	 */
	private native int getProcessedDimension();

	/**
	 * free any leftover malloced data or java references
	 */
	private native void cleanUpN();
}

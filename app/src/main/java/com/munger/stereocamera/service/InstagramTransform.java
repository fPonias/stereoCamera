package com.munger.stereocamera.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class InstagramTransform
{
	public static final int MAX_HEIGHT = 1350;
	public static final float RATIO = 4.0f / 5.0f;

	private Context context;
	private Listener listener;

	public InstagramTransform(Context c)
	{
		this.context = c;
	}

	public interface Listener
	{
		void onProcessed(File dest);
		void onFailed();
	}

	public void transform(final File source, final Listener listener)
	{
		this.listener = listener;
		Thread t = new Thread(new Runnable() { public void run()
		{
			Bitmap bmp = openScaledBitmap(source);
			final Bitmap ret = transformBitmap(bmp);
			bmp.recycle();

			writeTmpFile(ret, new WroteTmp() { public void onFileWritten(File file)
			{
				ret.recycle();
				listener.onProcessed(file);
			}});
		}});

		t.start();
	}

	private Bitmap openScaledBitmap(File source)
	{
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		Bitmap bmp = BitmapFactory.decodeFile(source.getPath(), options);
		int skip = (int) Math.ceil((double) options.outWidth / (double) MAX_HEIGHT);

		options = new BitmapFactory.Options();

		if (skip > 1)
			options.inSampleSize = skip;

		bmp = BitmapFactory.decodeFile(source.getPath(), options);
		return bmp;
	}

	private Bitmap transformBitmap(Bitmap bmp)
	{
		int oHeight = bmp.getHeight();
		int oWidth = bmp.getWidth();

		int nHeight = MAX_HEIGHT;
		int nWidth = (int) (MAX_HEIGHT * RATIO);
		Bitmap ret = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(ret);
		float scale = (float) nHeight / (float) oWidth;
		int tHeight = (int) (oWidth * scale);
		int tWidth = (int) (oHeight * scale);
		int diff = (nWidth - tWidth) / 2;

		Matrix transform = new Matrix();
		transform.postRotate(90);
		transform.postScale(scale, scale);
		transform.postTranslate(diff + tWidth, 0);

		Paint paint = new Paint();
		paint.setFilterBitmap(true);
		canvas.drawBitmap(bmp, transform, paint);

		return ret;
	}

	private static interface WroteTmp
	{
		void onFileWritten(File file);
	}

	private void writeTmpFile(final Bitmap bmp, final WroteTmp wroteTmp)
	{
		final PhotoFiles pf = new PhotoFiles(context);
		pf.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				FileOutputStream fos = null;

				try
				{
					File f = pf.getRandomFile();
					fos = new FileOutputStream(f);
					bmp.compress(Bitmap.CompressFormat.JPEG, 75, fos);

					wroteTmp.onFileWritten(f);
				}
				catch(IOException e){
					listener.onFailed();
				}
				finally
				{
					try
					{
						if (fos != null)
							fos.close();
					}
					catch(IOException e){}
				}
			}

			@Override
			public void fail()
			{
				listener.onFailed();
			}
		});
	}
}

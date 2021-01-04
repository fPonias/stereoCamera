package com.munger.stereocamera.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;

import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InstagramTransform
{


	public enum TransformType
	{
		COPY,
		ROTATE,
		SQUARE,
		SQUARE_ROTATE
	};

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

	public void transform(final PhotoFile source, final TransformType type, final Listener listener)
	{
		this.listener = listener;
		Thread t = new Thread(new Runnable() { public void run()
		{
			Transformer transform = TransformerFactory.create(type);

			if (transform == null)
            {
                listener.onFailed();
                return;
            }

			Bitmap ret = null;

			try (InputStream str = context.getContentResolver().openInputStream(source.uri);)
			{
				ret = transform.transform(str);
				writeTmpFile(ret, listener::onProcessed);
			}
			catch (IOException e) {
				listener.onFailed();
			}
			finally {
				if (ret != null)
					ret.recycle();
			}
		}});

		t.start();
	}

	private static abstract class Transformer
	{
		public static final int VERTICAL_MAX_HEIGHT = 1350;
		public static final float VERTICAL_RATIO = 4.0f / 5.0f;
		public static final int HORIZONTAL_MAX_WIDTH = 1080;
		public static final float HORIZONTAL_RATIO = 1.908f;
		public static final int SQUARE_DIM = 1080;
		protected Rect emptyRect = new Rect();

		abstract public Bitmap transform(InputStream str);
	}

	private static class TransformerFactory
	{
		public static Transformer create(TransformType type)
		{
			switch(type)
			{
				case ROTATE:
					return new RotateTransform();
				case COPY:
					return new CopyTransform();
				case SQUARE:
					return new SquareTransform();
				case SQUARE_ROTATE:
					return new SquareRotatedTransform();
				default:
					return null;
			}
		}
	}

	private static class RotateTransform extends Transformer
	{
		@Override
		public Bitmap transform(InputStream str)
		{
			Bitmap bmp = openScaledBitmap(str);
			Bitmap ret = transformBitmap(bmp);
			bmp.recycle();
			return ret;
		}

		private Bitmap openScaledBitmap(InputStream str)
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;

			int skip = (int) Math.ceil((double) options.outWidth / (double) VERTICAL_MAX_HEIGHT);

			options = new BitmapFactory.Options();

			if (skip > 1)
				options.inSampleSize = skip;

			Bitmap bmp = BitmapFactory.decodeStream(str, emptyRect, options);
			return bmp;
		}

		private Bitmap transformBitmap(Bitmap bmp)
		{
			int oHeight = bmp.getHeight();
			int oWidth = bmp.getWidth();

			int nHeight = VERTICAL_MAX_HEIGHT;
			int nWidth = (int) (VERTICAL_MAX_HEIGHT * VERTICAL_RATIO);
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


			Paint whitePaint = new Paint();
			whitePaint.setColor(Color.WHITE);
			canvas.drawRect(0, 0, nWidth, nHeight, whitePaint);

			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(bmp, transform, paint);

			return ret;
		}
	}

	private static class CopyTransform extends Transformer
	{
		@Override
		public Bitmap transform(InputStream source)
		{
			Bitmap bmp = openScaledBitmap(source);
			Bitmap ret = transformBitmap(bmp);
			bmp.recycle();
			return ret;
		}

		private Bitmap openScaledBitmap(InputStream source)
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			int skip = (int) Math.ceil((double) options.outWidth / (double) HORIZONTAL_MAX_WIDTH);

			options = new BitmapFactory.Options();

			if (skip > 1)
				options.inSampleSize = skip;

			Bitmap bmp = BitmapFactory.decodeStream(source, emptyRect, options);
			return bmp;
		}

		private Bitmap transformBitmap(Bitmap bmp)
		{
			int oHeight = bmp.getHeight();
			int oWidth = bmp.getWidth();

			int nHeight = (int) (HORIZONTAL_MAX_WIDTH / HORIZONTAL_RATIO);
			int nWidth = HORIZONTAL_MAX_WIDTH;
			Bitmap ret = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);

			Canvas canvas = new Canvas(ret);
			float scale = (float) nWidth / (float) oWidth;
			int tHeight = (int) (oHeight * scale);
			int tWidth = (int) (oWidth * scale);
			int diff = (nHeight - tHeight) / 2;

			Matrix transform = new Matrix();
			transform.postScale(scale, scale);
			transform.postTranslate(0, diff);

			Paint whitePaint = new Paint();
			whitePaint.setColor(Color.WHITE);
			canvas.drawRect(0, 0, nWidth, nHeight, whitePaint);

			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(bmp, transform, paint);

			return ret;
		}
	}

	private static class SquareTransform extends Transformer
	{
		@Override
		public Bitmap transform(InputStream source)
		{
			Bitmap bmp = openScaledBitmap(source);
			Bitmap ret = transformBitmap(bmp);
			bmp.recycle();
			return ret;
		}

		private Bitmap openScaledBitmap(InputStream source)
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			int skip = (int) Math.ceil((double) options.outWidth / (double) SQUARE_DIM);

			options = new BitmapFactory.Options();

			if (skip > 1)
				options.inSampleSize = skip;

			Bitmap bmp = BitmapFactory.decodeStream(source, emptyRect, options);
			return bmp;
		}

		private Bitmap transformBitmap(Bitmap bmp)
		{
			int oHeight = bmp.getHeight();
			int oWidth = bmp.getWidth();

			int nHeight = SQUARE_DIM;
			int nWidth = SQUARE_DIM;
			Bitmap ret = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);

			Canvas canvas = new Canvas(ret);
			float scale = (float) nWidth / (float) oWidth;

			Paint whitePaint = new Paint();
			whitePaint.setColor(Color.WHITE);
			canvas.drawRect(0, 0, nWidth, nHeight, whitePaint);

			Matrix transform = new Matrix();
			transform.postScale(scale, scale);
			transform.postTranslate(0, 0);

			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(bmp, transform, paint);

			transform = new Matrix();
			transform.postScale(scale, scale);
			transform.postTranslate(0, nHeight / 2);
			canvas.drawBitmap(bmp, transform, paint);

			return ret;
		}
	}

	private static class SquareRotatedTransform extends Transformer
	{
		@Override
		public Bitmap transform(InputStream source)
		{
			Bitmap bmp = openScaledBitmap(source);
			Bitmap ret = transformBitmap(bmp);
			bmp.recycle();
			return ret;
		}

		private Bitmap openScaledBitmap(InputStream source)
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			int skip = (int) Math.ceil((double) options.outWidth / (double) SQUARE_DIM);

			options = new BitmapFactory.Options();

			if (skip > 1)
				options.inSampleSize = skip;

			Bitmap bmp = BitmapFactory.decodeStream(source, emptyRect, options);
			return bmp;
		}

		private Bitmap transformBitmap(Bitmap bmp)
		{
			int oHeight = bmp.getHeight();
			int oWidth = bmp.getWidth();

			int nHeight = SQUARE_DIM;
			int nWidth = SQUARE_DIM;
			Bitmap ret = Bitmap.createBitmap(nWidth, nHeight, Bitmap.Config.ARGB_8888);

			Canvas canvas = new Canvas(ret);
			float scale = (float) nWidth / (float) oWidth;

			Paint whitePaint = new Paint();
			whitePaint.setColor(Color.WHITE);
			canvas.drawRect(0, 0, nWidth, nHeight, whitePaint);

			Matrix transform = new Matrix();
			transform.postScale(scale, scale);
			transform.postRotate(90);
			transform.postTranslate(nHeight / 2, 0);

			Paint paint = new Paint();
			paint.setFilterBitmap(true);
			canvas.drawBitmap(bmp, transform, paint);

			transform = new Matrix();
			transform.postScale(scale, scale);
			transform.postRotate(90);
			transform.postTranslate(nHeight, 0);
			canvas.drawBitmap(bmp, transform, paint);

			return ret;
		}
	}

	private static interface WroteTmp
	{
		void onFileWritten(File file);
	}

	private void writeTmpFile(final Bitmap bmp, final WroteTmp wroteTmp)
	{
		final PhotoFiles pf = PhotoFiles.Factory.get();
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
}

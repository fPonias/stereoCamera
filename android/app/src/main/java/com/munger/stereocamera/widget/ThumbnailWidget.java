package com.munger.stereocamera.widget;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.appcompat.widget.AppCompatImageView;

import android.graphics.Rect;
import android.util.AttributeSet;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by hallmarklabs on 3/12/18.
 */

public class ThumbnailWidget extends AppCompatImageView
{
	public ThumbnailWidget(Context context)
	{
		super(context);
	}

	public ThumbnailWidget(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public ThumbnailWidget(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		update();
	}

	private MainActivity.Listener appListener = new MainActivity.Listener()
	{
		public void onNewPhoto(String path)
		{
			update();
		}
	};

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		MainActivity.getInstance().removeListener(appListener);
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		MainActivity.getInstance().addListener(appListener);
	}

	public void update()
	{
		if (!MainActivity.getInstance().hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
			return;

		final PhotoFiles photoFiles = PhotoFiles.Factory.get();
		if (photoFiles.isEmpty())
			setImageDrawable(null);

		InputStream str = photoFiles.getNewestAsStream();

		if (str == null)
			return;

		str.mark(1024 * 1024);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(str, new Rect(), options);
		double maxw = 512;
		double picw = options.outWidth;
		int skip = (int) Math.ceil(picw / maxw);


		try{
			str.reset();
		}
		catch(IOException e){
			str = photoFiles.getNewestAsStream();
		}

		options.inSampleSize = skip;
		options.inJustDecodeBounds = false;
		Bitmap bmp = BitmapFactory.decodeStream(str, new Rect(), options);
		setImageBitmap(bmp);
	}
}

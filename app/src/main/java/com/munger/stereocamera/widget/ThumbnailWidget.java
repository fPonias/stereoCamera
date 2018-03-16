package com.munger.stereocamera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.munger.stereocamera.utility.PhotoFiles;

import java.io.File;

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

	public void update()
	{
		final PhotoFiles photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			public void done()
			{
				new Handler(Looper.getMainLooper()).post(new Runnable() { public void run()
				{
					File newest = photoFiles.getNewestFile();

					if (newest == null)
					{
						setImageDrawable(null);
					}
					else
					{
						String max = newest.getPath();
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inSampleSize = 8;
						Bitmap bmp = BitmapFactory.decodeFile(photoFiles.getFilePath(max), options);
						setImageBitmap(bmp);
					}
				}});
			}

			public void fail()
			{
				setImageBitmap(null);
			}
		});
	}
}

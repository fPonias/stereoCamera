package com.munger.stereocamera.widget;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.data.FileSystemViewModel;

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

		init(context);
	}

	public ThumbnailWidget(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		init(context);
	}

	public ThumbnailWidget(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);

		init(context);
	}

	private MutableLiveData<PhotoFile> mostRecent;

	private void init(Context context)
	{
		if (!(context instanceof LifecycleOwner))
		{
			Log.d("stereocamera", "context passed to ThumbnailWidget is not a LifecycleOwner");
			return;
		}

		LifecycleOwner c = (LifecycleOwner) context;
		FileSystemViewModel vm = MainActivity.getInstance().getFileSystemViewModel();
		mostRecent = vm.getMostRecentPhoto();
		mostRecent.observe(c, this::update);
	}

	public void update(PhotoFile photoFile)
	{
		if (!MainActivity.getInstance().hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
			return;

		if (photoFile == null)
			return;

		FileSystemViewModel vm = MainActivity.getInstance().getFileSystemViewModel();
		Bitmap bmp = vm.getPhotoFiles().getThumbnail(photoFile.id);
		setImageBitmap(bmp);
	}
}

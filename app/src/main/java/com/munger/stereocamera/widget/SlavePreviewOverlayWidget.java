package com.munger.stereocamera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class SlavePreviewOverlayWidget extends android.support.v7.widget.AppCompatImageView
{
	public SlavePreviewOverlayWidget(Context context)
	{
		super(context);
		init();
	}

	public SlavePreviewOverlayWidget(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public SlavePreviewOverlayWidget(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init()
	{
		setImageBitmap(null);
		setRotation(90);
		setScaleType(ImageView.ScaleType.CENTER_CROP);
		setAlpha(0.5f);
	}

	private String getLogTag() { return "slave preview overlay"; }

	public void cancel()
	{
		Log.d(getLogTag(), "cancelling slave overlay");
		synchronized (lock)
		{
			cancelled = true;
			lock.notify();

			if (runThread != null)
				try {lock.wait(250);} catch (InterruptedException e){}

			if (currentBmp != null)
			{
				currentBmp = null;
			}

			if (nextBmp != null)
			{
				nextBmp = null;
			}
		}

		Log.d(getLogTag(), "slave overlay cancelled");
		Handler h = new Handler(Looper.getMainLooper());
		h.post(new Runnable() { public void run()
		{
			setVisibility(View.GONE);
			setImageBitmap(null);
		}});
	}

	public boolean isRunning()
	{
		synchronized (lock)
		{
			return (!cancelled && runThread != null);
		}
	}

	private float zoom = 0f;
	private long lastFrameReceived = 0;
	private long hideTimeout = 1500;

	public void render(byte[] data, float zoom)
	{
		lastFrameReceived = System.currentTimeMillis();

		if (this.zoom != zoom)
		{
			this.zoom = zoom;
			setScaleX(zoom);
			setScaleY(zoom);
		}

		synchronized (lock)
		{
			if (isRendering || cancelled)
				return;
		}

		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
		Bitmap lastBmp = null;

		synchronized (lock)
		{
			if (nextBmp != currentBmp)
				lastBmp = nextBmp;

			nextBmp = bmp;
			lock.notify();
		}

		if (lastBmp != null && lastBmp != currentBmp)
			lastBmp.recycle();
	}

	private final Object lock = new Object();
	private Bitmap nextBmp;
	private Bitmap currentBmp;
	private boolean isRendering = false;
	private boolean cancelled = false;
	private Thread runThread = null;

	private Runnable previewUpdater = new Runnable() { public void run()
	{
		long now = System.currentTimeMillis();
		long diff = now - lastFrameReceived;

		if (diff > hideTimeout)
		{
			setVisibility(View.GONE);
			return;
		}
		else
		{
			setVisibility(View.VISIBLE);
		}

		Bitmap lastBmp = null;

		synchronized (lock)
		{
			if (nextBmp != null && !nextBmp.isRecycled())
			{
				if (currentBmp != null)
					lastBmp = currentBmp;

				currentBmp = nextBmp;
			}
		}


		setImageBitmap(currentBmp);
		if (lastBmp != null && lastBmp != currentBmp)
			lastBmp.recycle();
	}};

	private Runnable process = new Runnable() { public void run()
	{
		lastFrameReceived = System.currentTimeMillis();
		Handler h = new Handler(Looper.getMainLooper());
		nextBmp = null;

		h.post(previewUpdater);

		while (!cancelled)
		{
			synchronized (lock)
			{
				try {lock.wait(hideTimeout);} catch(InterruptedException e){break;}

				if (cancelled)
					break;

				isRendering = true;
			}

			h.post(previewUpdater);

			synchronized (lock)
			{
				isRendering = false;

				if (System.currentTimeMillis() - lastFrameReceived > hideTimeout)
				{
					break;
				}
			}
		}

		synchronized (lock)
		{
			cancelled = true;
			runThread = null;
			lock.notify();
		}
	}};

	public void start()
	{
		Log.d(getLogTag(), "starting slave overlay");
		synchronized (lock)
		{
			if (runThread != null)
				return;

			cancelled = false;
			runThread = new Thread(process);
			runThread.start();
		}
		Log.d(getLogTag(), "slave overlay started");
	}
}

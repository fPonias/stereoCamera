package com.munger.stereocamera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class AudioSyncWidget extends View
{
	public AudioSyncWidget(Context context)
	{
		super(context);
		init();
	}

	public AudioSyncWidget(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public AudioSyncWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init();
	}
	
	protected void init()
	{
		backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		backgroundPaint.setStrokeWidth(1.0f);

		triggerPaint = new Paint();
		triggerPaint.setColor(Color.BLACK);

		samplePaint = new Paint();
		samplePaint.setColor(Color.BLUE);
		samplePaint.setStrokeWidth(1.0f);

		thresholdPaint = new Paint();
		thresholdPaint.setColor(Color.RED);
		thresholdPaint.setPathEffect(new DashPathEffect(new float[] {5.0f, 5.0f}, 0));
		thresholdPaint.setStrokeWidth(1.0f);

		handler = new Handler(Looper.getMainLooper());
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		drawBackground(canvas);
		drawSamples(canvas);
		drawThreshold(canvas);

		synchronized (drawLock)
		{
			drawLock.notify();
		}
	}

	protected boolean isAttached = false;

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		synchronized (lock)
		{
			if (isAttached)
				return;

			isAttached = true;
		}

		Thread t = new Thread(new Runnable() { public void run()
		{
			bufferReader();
		}});
		t.start();
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		synchronized (lock)
		{
			if (!isAttached)
				return;

			isAttached = false;
			lock.notify();
		}

		synchronized (drawLock)
		{
			drawLock.notify();
		}
	}

	protected int width;
	protected int height;
	
	protected Paint backgroundPaint;
	protected Paint triggerPaint;
	protected Paint samplePaint;
	protected Paint thresholdPaint;

	protected float sampleMax = Short.MAX_VALUE;
	protected int sampleArraySz = 1000;
	protected int sampleArrayCount = 0;
	protected int bufferMax = 100;
	protected float[] buffer = new float[bufferMax];
	protected int bufferIdx = 0;
	protected LinkedList<Float> sampleArray = new LinkedList<>();
	protected float threshold;

	protected float triggerCurrentOpacity = 0.0f;
	protected float triggerOpacity = 0.3f;
	protected long triggerFadeDuration = 500;
	protected long triggerTimestamp = 0;

	protected Handler handler;
	protected final Object lock = new Object();
	protected final Object drawLock = new Object();

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);

		width = w;
		height = h;
	}

	public void putSample(float sample)
	{
		synchronized (lock)
		{
			if (bufferIdx >= bufferMax)
			{
				bufferIdx++;

				if (bufferIdx == bufferMax)
					Log.d("AudioSyncWidget", "buffer overrun");

				lock.notify();

				return;
			}

			buffer[bufferIdx] = sample;
			bufferIdx++;

			lock.notify();
		}
	}

	public void triggered()
	{
		synchronized (lock)
		{
			triggerCurrentOpacity = triggerOpacity;
			triggerTimestamp = System.currentTimeMillis();

			lock.notify();
		}
	}

	protected void bufferReader()
	{
		while(isAttached)
		{
			synchronized (lock)
			{
				if (bufferIdx == 0)
					try {lock.wait();} catch(InterruptedException e){}

				if (!isAttached)
					break;

				if (bufferIdx > 0)
				{
					int max = Math.min(bufferMax, bufferIdx);
					for (int i = 0; i < max; i++)
					{
						float sample = buffer[i];
						if (sampleArrayCount == sampleArraySz)
						{
							sampleArray.removeFirst();
							sampleArrayCount--;
						}

						sampleArray.add(sample);
						sampleArrayCount++;
					}

					bufferIdx = 0;
				}

				long now = System.currentTimeMillis();
				long diff = now - triggerTimestamp;
				if (diff < triggerFadeDuration)
				{
					float ratio = 1.0f - ((float) diff / (float) triggerFadeDuration);
					triggerCurrentOpacity = triggerOpacity * ratio;
				}
				else
					triggerCurrentOpacity = 0.0f;
			}

			handler.post(sampleR);
			synchronized (drawLock)
			{
				try {drawLock.wait();} catch(InterruptedException e){}
			}
		}
	}

	protected Runnable sampleR = new Runnable()
	{
		@Override
		public void run()
		{
			invalidate();
		}
	};

	public void setThreshold(float threshold)
	{
		this.threshold = threshold;
		sampleMax = threshold * 2;
	}

	protected void drawBackground(Canvas canvas)
	{
		canvas.drawLine(0,0,0, height, backgroundPaint);
		canvas.drawLine(0, height, width, height, backgroundPaint);
		canvas.drawLine(width,0, width, height, backgroundPaint);
		canvas.drawLine(0,0, width,0, backgroundPaint);

		if (triggerCurrentOpacity > 0)
		{
			int alpha = (int) (255.0f * triggerCurrentOpacity);
			triggerPaint.setAlpha(alpha);
			canvas.drawRect(0, 0, width, height, triggerPaint);
		}
	}

	protected void drawSamples(Canvas canvas)
	{
		if (sampleArrayCount < 2)
			return;

		float xStep = (float) width / sampleArraySz;
		float yStep = (float) height / sampleMax;
		int x1, x2, y1, y2;

		synchronized (drawLock)
		{
			ListIterator<Float> iter = sampleArray.listIterator();
			float last = iter.next();
			x2 = 0;
			y2 = (int) Math.max(0, Math.min(last * yStep, height));

			while (iter.hasNext())
			{
				int idx = iter.nextIndex();
				float next = iter.next();

				x1 = x2;
				y1 = y2;
				x2 = (int)((idx - 1) * xStep);
				y2 = (int) Math.max(0, Math.min(next * yStep, height));

				canvas.drawLine(x1, height - y1, x2, height - y2, samplePaint);
			}
		}
	}

	protected void drawThreshold(Canvas canvas)
	{
		float yStep = (float) height / sampleMax;
		int y = (int) (yStep * threshold);
		canvas.drawLine(0, height - y, width, height - y, thresholdPaint);
	}
}

package com.munger.stereocamera.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;import com.munger.stereocamera.R;



public class LoadingWidget extends View
{
	public LoadingWidget(Context context)
	{
		super(context);
		setup();
	}

	public LoadingWidget(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
		setup();
	}

	public LoadingWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		setup();
	}



	private void setup()
	{
		density = Resources.getSystem().getDisplayMetrics().density;

		glasses = getResources().getDrawable(R.drawable.glasses);
		glow = getResources().getDrawable(R.drawable.glasses_glow);
		background = new Paint();
		background.setStyle(Paint.Style.FILL);
		background.setColor(Color.argb(200, 255, 255 ,255));

		glassesSz = dpToPx(20);

		circlePaint = new Paint();
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(Color.rgb(0, 0 , 0));

		circleBigRadius = dpToPx(30);
		circleSmallRadius = dpToPx(5);

		textPaint = new Paint();
		textPaint.setStyle(Paint.Style.STROKE);
		textPaint.setColor(Color.rgb(0, 0, 0));
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(dpToPx(13));

		textOffset = dpToPx(50);

		start();
	}

	private int dpToPx(float dp)
	{
		return (int) (dp * density);
	}

	private Runnable invalidateRunnable = new Runnable() { public void run()
	{
		invalidate();
	}};

	public void start()
	{
		if (!cancelled)
			return;

		cancelled = false;

		Thread t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				LoadingWidget.this.run();
			}
		});
		t.start();
	}

	private void run()
	{
		while(!cancelled)
		{
			try{Thread.sleep(10);}catch(InterruptedException e){return;}

			updateGlow();
			updateCircles();

			Handler h = getHandler();

			if (h == null)
				return;

			h.post(invalidateRunnable);
		}
	}

	private boolean cancelled = true;
	private float density;

	public void stop()
	{
		cancelled = true;
	}

	private void updateGlow()
	{
		glowTransparency += glowStep * glowBrighter;

		if (glowTransparency >= glowMax)
		{
			glowBrighter = -1;
			glowTransparency = glowMax;
		}
		else if (glowTransparency <= glowMin)
		{
			glowBrighter = 1;
			glowTransparency = glowMin;
		}
	}

	private void updateCircles()
	{
		long now = System.currentTimeMillis();
		long diff = now - circleStepDelayTick;

		if (diff >= circleStepDelay)
		{
			circleRotateStart = (circleRotateStart + circleRotateStep) % 360.0f;
			circleStepDelayTick = now;
		}
	}

	private Drawable glasses;
	private Drawable glow;
	private Paint background;

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		Resources res = getContext().getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		int hret = dm.heightPixels;

		int id = res.getIdentifier("status_bar_height", "dimen", "android");
		if (id > 0)
		{
			int h = res.getDimensionPixelSize(id);
			hret -= h;
		}

		TypedValue tv = new TypedValue();
		if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
		{
			int h = TypedValue.complexToDimensionPixelSize(tv.data, res.getDisplayMetrics());
			hret -= h;
		}

		setMeasuredDimension(dm.widthPixels, hret);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		int centerw = getMeasuredWidth() / 2;
		int centerh = getMeasuredHeight() / 2;

		canvas.drawPaint(background);
		drawGlasses(centerw, centerh, canvas);
		drawCircles(centerw, centerh, canvas);
		drawText(centerw, centerh, canvas);
	}

	private int glassesSz = 100;
	private int glowTransparency = 0;
	private int glowStep = 1;
	private int glowMax = 255;
	private int glowMin = 0;
	private int glowBrighter = 1;

	private void drawGlasses(int centerw, int centerh, Canvas canvas)
	{
		int left = centerw - glassesSz;
		int right = centerw + glassesSz;
		int top = centerh - glassesSz;
		int bottom = centerh + glassesSz;

		glasses.setBounds(left, top, right, bottom);
		glasses.draw(canvas);
		glow.setAlpha(glowTransparency);
		glow.setBounds(left, top, right, bottom);
		glow.draw(canvas);
	}

	private int circleCount = 6;
	private int circleBigRadius = 120;
	private int circleSmallRadius = 15;
	private Paint circlePaint;
	private int circleStartIdx = 0;
	private int circleAlphaStep = 30;
	private float circleRotateStart = 0.0f;
	private float circleRotateStep = 5.0f;
	private long circleStepDelay = 50;
	private long circleStepDelayTick = 0;

	private void drawCircles(int centerw, int centerh, Canvas canvas)
	{
		Matrix rotateM = new Matrix();

		float[] center = {0, circleBigRadius};
		rotateM.postRotate(circleRotateStart);
		rotateM.mapPoints(center);

		rotateM = new Matrix();
		rotateM.postRotate(360.0f / circleCount);

		int alpha = 255;

		for (int i = 0; i < circleCount; i++)
		{
			circlePaint.setAlpha(alpha);
			canvas.drawCircle(center[0] + centerw, center[1] + centerh, circleSmallRadius, circlePaint);

			alpha = Math.min(255, alpha + circleAlphaStep);
			rotateM.mapPoints(center);
		}
	}

	private String text = "Test";

	public void setText(final String text)
	{
		LoadingWidget.this.text = text;

		Handler h = getHandler();

		if (h == null)
			return;

		h.post(new Runnable() { public void run()
		{
			invalidate();
		}});
	}

	private Paint textPaint;
	private int textOffset = 200;

	private void drawText(int centerw, int centerh, Canvas canvas)
	{
		canvas.drawText(text, centerw, centerh + textOffset, textPaint);
	}
}

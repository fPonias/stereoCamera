package com.munger.stereocamera.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.munger.stereocamera.R;


/**
 * Created by hallmarklabs on 3/19/18.
 */

public class PreviewOverlayWidget extends View
{
	public PreviewOverlayWidget(Context context)
	{
		super(context);

		paint = new Paint();
		paint.setColor(color);
	}

	public PreviewOverlayWidget(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);

		parseAttributes(attrs);
	}

	public PreviewOverlayWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);

		parseAttributes(attrs);
	}

	public enum Type
	{
		None,
		Crosshairs,
		Thirds,
		Fourths
	};

	private Type type = Type.None;
	private int color = Color.WHITE;
	private Paint paint;

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
		Handler h = getHandler();
		if (h != null)
		{
			h.post(new Runnable() { public void run()
			{
				invalidate();
			}});
		}
	}

	public int getColor()
	{
		return color;
	}

	public void setColor(int color)
	{
		this.color = color;
		paint.setColor(color);

		invalidate();
	}

	private void parseAttributes(AttributeSet attrs)
	{
		TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.PreviewOverlayWidget, 0, 0);

		try
		{
			int type = a.getInteger(R.styleable.PreviewOverlayWidget_type, 0);
			this.type = Type.values()[type];
			color = a.getColor(R.styleable.PreviewOverlayWidget_color, Color.WHITE);

			paint = new Paint();
			paint.setColor(color);
		}
		finally{
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		switch(type)
		{
			case None:
				break;
			case Crosshairs:
				drawCrosshairs(canvas);
				break;
			case Thirds:
				drawThirds(canvas);
				break;
			case Fourths:
				drawFourths(canvas);
				break;
		}
	}

	private void drawCrosshairs(Canvas canvas)
	{
		drawEvenLines(canvas, 1);
	}

	private void drawThirds(Canvas canvas)
	{
		drawEvenLines(canvas, 2);
	}

	private void drawFourths(Canvas canvas)
	{
		drawEvenLines(canvas, 3);
	}

	private void drawEvenLines(Canvas canvas, int dividers)
	{
		int w = canvas.getWidth();
		int h = canvas.getHeight();
		int spacex = w / (dividers + 1);
		int spacey = h / (dividers + 1);

		for (int i = 0; i < dividers; i++)
		{
			int y = spacey * (i + 1);
			int x = spacex * (i + 1);
			canvas.drawLine(0, y, w, y, paint);
			canvas.drawLine(x, 0, x, h, paint);
		}
	}
}

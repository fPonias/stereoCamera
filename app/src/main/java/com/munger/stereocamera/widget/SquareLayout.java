package com.munger.stereocamera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.RelativeLayout;

/**
 * Created by hallmarklabs on 3/8/18.
 */

public class SquareLayout extends RelativeLayout
{
	public SquareLayout(Context context)
	{
		super(context);
	}

	public SquareLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public SquareLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		super.onLayout(changed, l, t, r, b);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int min = Math.min(widthMeasureSpec, heightMeasureSpec);
		int sz = MeasureSpec.getSize(min);
		setMeasuredDimension(sz, sz);
	}
}

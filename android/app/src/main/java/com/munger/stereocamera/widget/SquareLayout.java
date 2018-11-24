package com.munger.stereocamera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Created by hallmarklabs on 3/8/18.
 */

public class SquareLayout extends ViewGroup
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
		if (!changed)
			return;

		int dim = getMeasuredWidth();

		int sz = getChildCount();
		for (int i = 0; i < sz; i++)
		{
			View child = getChildAt(i);

			child.layout(0,0, dim, dim);
			child.requestLayout();
		}
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

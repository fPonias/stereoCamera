package com.munger.stereocamera.fragment;

import android.content.Context;
import android.graphics.Color;

import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class DiscoverDialogItem extends AppCompatTextView
{
	public DiscoverDialogItem(Context context)
	{
		super(context);
	}

	public DiscoverDialogItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public DiscoverDialogItem(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		boolean ret = super.onTouchEvent(event);

		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN)
		{
			setBackgroundColor(Color.argb(255, 200, 200, 200));
		}
		else if (action == MotionEvent.ACTION_UP)
		{
			setBackgroundColor(Color.argb(0, 255, 255, 255));
		}

		return ret;
	}
}

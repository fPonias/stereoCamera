package com.munger.stereocamera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.Nullable;import com.munger.stereocamera.R;

/**
 * Created by hallmarklabs on 3/22/18.
 */

public class ZoomWidget extends LinearLayout
{

	public ZoomWidget(Context context)
	{
		super(context);
		init();
	}

	public ZoomWidget(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public ZoomWidget(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		init();
	}

	private View root;
	private SeekBar zoomSlider;
	private Listener listener;

	public static float MAX_ZOOM = 2.0f;

	private void init()
	{
		root = inflate(getContext(), R.layout.widget_zoom, this);
		zoomSlider = root.findViewById(R.id.slider);

		zoomSlider.setOnSeekBarChangeListener(new ZoomListener());
		int idx = convertValue(MAX_ZOOM);
		zoomSlider.setMax(idx);
	}

	public interface Listener
	{
		void onChange(float value);
	}

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}

	private class ZoomListener implements SeekBar.OnSeekBarChangeListener
	{
		private boolean touched = false;

		@Override
		public void onProgressChanged(SeekBar seekBar, int i, boolean b)
		{
			if (touched)
			{
				float value = get();

				if (listener != null)
					listener.onChange(value);
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar)
		{
			touched = true;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar)
		{
			touched = false;

			float value = get();
		}
	}

	public void set(float zoom)
	{
		int idx = convertValue(zoom);
		zoomSlider.setProgress(idx);
	}

	private int convertValue(float value)
	{
		int idx = (int)(value * 100.0f - 100.0f);
		idx = Math.max(0, Math.min(idx, zoomSlider.getMax()));
		return idx;
	}

	public float get()
	{
		int idx = zoomSlider.getProgress();
		return convertValue(idx);
	}

	private float convertValue(int value)
	{
		float ret = ((float) value + 100.0f) / 100.0f;
		return ret;
	}
}

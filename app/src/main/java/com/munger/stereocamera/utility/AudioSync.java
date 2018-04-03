package com.munger.stereocamera.utility;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.munger.stereocamera.widget.AudioSyncWidget;

public class AudioSync extends AudioAnalyser
{
	private short threshold;
	private int triggerLength = 600;
	private int peakSampleWidth = 150;

	private int thresholdStartIdx = 0;
	private int thresholdCurrentIdx = 0;
	private int max = 0;
	private int sampleIdx = 0;
	private boolean triggered = false;
	private boolean maxFlipped = false;

	private Listener listener;
	private AudioSyncWidget widget;

	public void setWidget(AudioSyncWidget widget)
	{
		this.widget = widget;
		widget.setThreshold(threshold);
	}

	protected String getTag() { return getClass().getName(); };

	public void start(Listener listener, short threshold)
	{
		this.threshold = threshold;
		this.listener = listener;

		start();
	}

	@Override
	public void analyzeBuffer(short[] buffer, int read)
	{
		int sample;

		for (int i = 0; i < read; i++)
		{
			sample = Math.abs(buffer[i]);

			if (sample > max)
				max = sample;

			sampleIdx++;
			if (sampleIdx == peakSampleWidth)
			{
				onSample(i);

				sampleIdx = 0;
				max = 0;
			}
		}
	}

	private void onSample(int idx)
	{
		if(debugging)
			Log.d(getTag(), "sample with max " + max + " read");

		if (widget != null)
			widget.putSample(max);

		if (max >= threshold)
		{
			if (!maxFlipped)
			{
				thresholdStartIdx = idx;
				maxFlipped = true;
				thresholdCurrentIdx = thresholdStartIdx;
			}
			else
				thresholdCurrentIdx += peakSampleWidth;

			int diff = thresholdCurrentIdx - thresholdStartIdx;
			if (diff > triggerLength && !triggered)
			{
				triggered = true;
				trigger();
			}
		}
		else
		{
			triggered = false;
			maxFlipped = false;
		}
	}

	private void trigger()
	{
		if (widget != null)
			widget.triggered();

		if (listener != null)
			listener.trigger();
	}

	public interface Listener
	{
		void trigger();
	}
}

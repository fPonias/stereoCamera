package com.munger.stereocamera.utility;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

public class AudioAmbientAnalysis extends AudioAnalyser
{
	@Override
	protected String getTag()
	{
		return this.getClass().getName();
	}

	private double analyseMean = 0;
	private double analyseStdDevSquared = 0;
	private double analyseCount = 0;

	@Override
	public void analyzeBuffer(short[] buffer, int read)
	{
		int value;
		for (int i = 0; i < read; i++)
		{
			value = Math.abs(buffer[i]);
			doCalculations(value);
		}
	}

	private void doCalculations(double value)
	{
		double prev = analyseMean;
		analyseCount++;
		analyseMean = analyseMean + (value - analyseMean) / analyseCount;
		analyseStdDevSquared = analyseStdDevSquared + (value - analyseMean) * (value - prev);
	}

	public long getMean()
	{
		return (long) analyseMean;
	}

	public long getStdDev()
	{
		if (analyseCount > 0)
			return (long) Math.sqrt(analyseStdDevSquared / analyseCount);
		else
			return 0;
	}

	public long getCount()
	{
		return (long) analyseCount;
	}

	public void execute()
	{
		super.start();
	}

	public void reset()
	{
		analyseCount = 0;
		analyseMean = 0;
		analyseStdDevSquared = 0;
	}
}

package com.munger.stereocamera.utility;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.widget.AudioSource;
import com.munger.stereocamera.widget.AudioSyncWidget;

public abstract class AudioAnalyser implements AudioSource.Listener
{
	protected boolean debugging = false;

	public void setDebugging(boolean value)
	{
		this.debugging = value;
	}
	protected void start()
	{
		MyApplication.getInstance().getAudioSource().setListener(this);
	}

	public void stop()
	{
		MyApplication.getInstance().getAudioSource().setListener(null);
	}

	abstract public void analyzeBuffer(short[] buffer, int read);
	abstract protected String getTag();
}

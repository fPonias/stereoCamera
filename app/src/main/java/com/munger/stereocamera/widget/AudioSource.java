package com.munger.stereocamera.widget;

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

/**
 * Created by hallmarklabs on 4/3/18.
 */

public class AudioSource
{
	protected static final int SAMPLE_RATE = 44100;
	protected static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
	protected static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	protected static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING) / 2;

	protected static AudioRecord audioRecord;

	protected boolean debugging = false;

	protected Context parent;

	protected final Object lock = new Object();
	protected boolean cancelled = false;
	protected short[] buffer = new short[BUFFER_SIZE];

	public AudioSource(Context parent)
	{
		this.parent = parent;
	}

	public void start()
	{
		synchronized (lock)
		{
			if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
				return;
		}

		int hasPermission = ContextCompat.checkSelfPermission(parent, Manifest.permission.RECORD_AUDIO);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && hasPermission != PackageManager.PERMISSION_GRANTED)
		{
			BaseActivity a = MyApplication.getInstance().getCurrentActivity();
			a.requestPermissionForResult(Manifest.permission.RECORD_AUDIO, new MainActivity.PermissionResultListener()
			{
				@Override
				public void onResult(int resultCode)
				{
					start();
				}
			});
			return;
		}

		audioRecord = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE * 2
		);
		audioRecord.startRecording();

		synchronized (lock)
		{
			if (thread != null)
				return;

			cancelled = false;
			thread = new Thread(bufferReader);
		}

		thread.start();
	}

	public void stop()
	{
		synchronized (lock)
		{
			cancelled = true;

			if (thread != null)
				try {lock.wait(1000);}catch(InterruptedException e){}

			audioRecord.stop();
			audioRecord.release();
		}
	}

	private String getTag() {return getClass().getName();}

	private Thread thread;
	private Runnable bufferReader = new Runnable() { public void run()
	{
		if (audioRecord == null)
		{
			audioRecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC,
					SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE * 2
			);
			audioRecord.startRecording();
		}

		int read;

		while(true)
		{
			synchronized (lock)
			{
				if (cancelled)
				{
					thread = null;
					lock.notify();
					break;
				}
			}

			read = audioRecord.read(buffer, 0, BUFFER_SIZE);

			if (debugging)
				Log.d(getTag(), "read " + read + " bytes from audio buffer");

			if (read > 0 && listener != null)
				listener.analyzeBuffer(buffer, read);
		}
	}};

	public interface Listener
	{
		void analyzeBuffer(short[] buffer, int read);
	}

	private Listener listener;

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}
}

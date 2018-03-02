package com.munger.stereocamera.fragment;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class SlaveFragment extends PreviewFragment
{
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View ret = inflater.inflate(R.layout.fragment_slave, null);
		super.onCreateView(ret);

		zoomBar = ret.findViewById(R.id.seekBar);

		return ret;
	}

	private BluetoothSlaveComm slaveComm;
	private byte[] imgBytes;

	private SeekBar zoomBar;

	@Override
	protected void updatePreviewParameters()
	{
		super.updatePreviewParameters();

		zoomBar.setMax(maxZoom);
		zoomBar.setProgress(currentZoom);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		zoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b)
			{
				currentZoom = i;

				Camera.Parameters parameters = camera.getParameters();
				parameters.setZoom(i);
				camera.setParameters(parameters);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{

			}
		});

		try
		{
			slaveComm = MyApplication.getInstance().getBtCtrl().getSlave().getComm();
		}
		catch (NullPointerException e){
			return;
		}

		slaveComm.setListener(new BluetoothSlaveComm.CommandListener()
		{
			@Override
			public int onStatus()
			{
				return status;
			}

			@Override
			public void onPing()
			{

			}

			@Override
			public byte[] onFireShutter()
			{
				return doFireShutter();
			}

			@Override
			public void onLatencyCheck()
			{
				doLatencyCheck();
			}

			@Override
			public void onFlip()
			{
				doFlip();
			}

			@Override
			public float[] getViewAngles()
			{
				return new float[] {horizontalAngle, verticalAngle};
			}

			@Override
			public void setZoom(float zoom)
			{
				SlaveFragment.this.setZoom(null, zoom);
			}
		});
	}

	@Override
	protected void setZoom(Camera.Parameters parameters, float zoom)
	{
		if (parameters == null)
			parameters = camera.getParameters();

		super.setZoom(parameters, zoom);

		zoomBar.setProgress(parameters.getZoom());
	}

	private void doLatencyCheck()
	{
		final Object lock = new Object();

		getLatency(new LatencyListener()
		{
			@Override
			public void triggered()
			{
				synchronized (lock)
				{
					lock.notify();
				}
			}

			@Override
			public void done()
			{
			}
		});

		synchronized (lock)
		{
			try{lock.wait(3000);}catch(InterruptedException e){}
		}
	}

	private byte[] doFireShutter()
	{
		final Object lock = new Object();
		imgBytes = null;

		fireShutter(new ImageListener()
		{
			@Override
			public void onImage(byte[] bytes)
			{
				synchronized (lock)
				{
					imgBytes = bytes;
					lock.notify();
				}
			}
		});

		synchronized (lock)
		{
			if (imgBytes == null)
				try{lock.wait(2000);}catch(InterruptedException e){}
		}

		return imgBytes;
	}
}

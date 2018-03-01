package com.munger.stereocamera.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.BluetoothSlaveComm;

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

		zoomInButton = ret.findViewById(R.id.zoom_in);
		zoomOutButton = ret.findViewById(R.id.zoom_out);

		return ret;
	}

	private BluetoothSlaveComm slaveComm;
	private byte[] imgBytes;

	private Button zoomInButton;
	private Button zoomOutButton;

	@Override
	public void onStart()
	{
		super.onStart();

		zoomInButton.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			zoom(true);
		}});

		zoomOutButton.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			zoom(false);
		}});

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

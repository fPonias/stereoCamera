package com.munger.stereocamera.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.munger.stereocamera.MainActivity;
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
		return ret;
	}

	private BluetoothSlaveComm slaveComm;
	private byte[] imgBytes;

	@Override
	public void onStart()
	{
		super.onStart();

		slaveComm = MainActivity.getInstance().getBtCtrl().getSlave().getComm();
		slaveComm.setListener(new BluetoothSlaveComm.CommandListener()
		{
			@Override
			public void onPing()
			{

			}

			@Override
			public byte[] onFireShutter()
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
		});
	}
}

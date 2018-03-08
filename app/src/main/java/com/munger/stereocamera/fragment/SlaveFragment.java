package com.munger.stereocamera.fragment;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;
import com.munger.stereocamera.bluetooth.command.slave.commands.GetLatency;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveFacing;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveZoom;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendAngleOfView;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendGravity;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendOrientation;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendStatus;
import com.munger.stereocamera.bluetooth.command.slave.commands.Shutter;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;
import com.munger.stereocamera.widget.OrientationCtrl;

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

		controls = ret.findViewById(R.id.controls);
		zoomSlider = ret.findViewById(R.id.zoom_slider);

		zoomSlider.setOnSeekBarChangeListener(new ZoomListener());

		return ret;
	}

	private class ZoomListener implements SeekBar.OnSeekBarChangeListener
	{
		private boolean touched = false;

		@Override
		public void onProgressChanged(SeekBar seekBar, int i, boolean b)
		{
			if (touched)
				setZoom(null, i);
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
		}
	}

	private BluetoothSlaveComm slaveComm;
	private byte[] imgBytes;
	private OrientationCtrl gravityCtrl;

	private ViewGroup controls;
	private SeekBar zoomSlider;

	private long lastSent = 0;
	private final Object gravityLock = new Object();
	private boolean sending = false;


	@Override
	protected void updatePreviewParameters()
	{
		super.updatePreviewParameters();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		gravityCtrl = new OrientationCtrl();
		gravityCtrl.setChangeListener(new OrientationCtrl.ChangeListener()
		{
			@Override
			public void change(float[] values)
			{
				synchronized (gravityLock)
				{
					long diff = System.currentTimeMillis() - lastSent;

					if (sending || diff < 200)
						return;

					sending = true;
				}

				ReceiveGravity.Gravity value = new ReceiveGravity.Gravity();
				value.x = values[0];
				value.y = values[1];
				value.z = values[2];
				SendGravity command = new SendGravity(value);
				command.setListener(new SlaveCommand.Listener()
				{
					@Override
					public void onStarted()
					{}

					@Override
					public void onFinished()
					{
						synchronized (gravityLock)
						{
							lastSent = System.currentTimeMillis();
							sending = false;
						}
					}
				});
				slaveComm.sendCommand(command);
			}
		});
		gravityCtrl.start();

		try
		{
			slaveComm = MyApplication.getInstance().getBtCtrl().getSlave().getComm();
		}
		catch (NullPointerException e){
			return;
		}

		slaveComm.addListener(BluetoothCommands.SET_ZOOM, new BluetoothSlaveComm.Listener()
		{
			@Override
			public void onCommand(SlaveCommand command)
			{
				ReceiveZoom zoomCommand = (ReceiveZoom) command;
				SlaveFragment.this.setZoom(null, zoomCommand.getZoom());
			}
		});

		slaveComm.addListener(BluetoothCommands.SET_FACING, new BluetoothSlaveComm.Listener()
		{
			@Override
			public void onCommand(SlaveCommand command)
			{
				ReceiveFacing facingCommand = (ReceiveFacing) command;
				SlaveFragment.this.setFacing(facingCommand.getFacing());
			}
		});

		slaveComm.addListener(BluetoothCommands.LATENCY_CHECK, new BluetoothSlaveComm.Listener()
		{
			@Override
			public void onCommand(SlaveCommand command)
			{
				long now = System.currentTimeMillis();
				doLatencyCheck();
				long then = System.currentTimeMillis();
				long diff = then - now;

				((GetLatency) command).setLatency(diff);
			}
		});

		slaveComm.addListener(BluetoothCommands.FIRE_SHUTTER, new BluetoothSlaveComm.Listener()
		{
			@Override
			public void onCommand(SlaveCommand command)
			{

				Shutter cmd = (Shutter) command;

				cmd.setOrientation(getCurrentOrientation());
				cmd.setZoom(getZoomValue());

				byte[] data = doFireShutter();
				cmd.setData(data);
			}
		});

		slaveComm.addListener(BluetoothCommands.PING, new BluetoothSlaveComm.Listener()
		{
			@Override
			public void onCommand(SlaveCommand command)
			{}
		});

		slaveComm.sendCommand(new SendOrientation(orientation));
	}

	@Override
	protected void adjustCrop(Camera.Parameters parameters)
	{
		super.adjustCrop(parameters);

		DisplayMetrics dp = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dp);
		int preWidth = previewView.getMeasuredWidth();
		int controlHeight = dp.heightPixels - preWidth;

		ViewGroup.LayoutParams params = controls.getLayoutParams();
		params.height = controlHeight;
		params.width = dp.widthPixels;

		controls.setLayoutParams(params);
	}

	@Override
	protected void setZoom(Camera.Parameters parameters, float zoom)
	{
		if (parameters == null)
			parameters = camera.getParameters();

		super.setZoom(parameters, zoom);

		zoomSlider.setProgress(currentZoom);
	}

	private boolean latencyCheckDone = false;
	private void doLatencyCheck()
	{
		final Object lock = new Object();
		latencyCheckDone = false;

		Thread t = new Thread(new Runnable() { public void run()
		{
			getLatency(0, new LatencyListener()
			{
				@Override
				public void triggered()
				{
					synchronized (lock)
					{
						latencyCheckDone = true;
						lock.notify();
					}
				}

				@Override
				public void done()
				{}

				public void fail()
				{
					synchronized (lock)
					{
						latencyCheckDone = true;
						lock.notify();
					}
				}
			});
		}});
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();

		synchronized (lock)
		{
			if (latencyCheckDone == false)
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

	@Override
	protected void setStatus(Status status)
	{
		super.setStatus(status);

		if (slaveComm != null)
		{
			Log.d("Slave", "sending " + status.name() + " status");
			slaveComm.sendCommand(new SendStatus(status));
		}
	}

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();
		slaveComm.sendCommand(new SendAngleOfView(verticalAngle, horizontalAngle));

		zoomSlider.setMax(maxZoom);
		zoomSlider.setProgress(currentZoom);
	}
}

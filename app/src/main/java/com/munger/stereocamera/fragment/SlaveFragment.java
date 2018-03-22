package com.munger.stereocamera.fragment;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;
import com.munger.stereocamera.bluetooth.command.slave.commands.GetLatency;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveFacing;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveOverlay;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveProcessedPhoto;
import com.munger.stereocamera.bluetooth.command.slave.commands.ReceiveZoom;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendAngleOfView;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendGravity;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendOrientation;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendPreviewFrame;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendStatus;
import com.munger.stereocamera.bluetooth.command.slave.commands.Shutter;
import com.munger.stereocamera.bluetooth.command.slave.SlaveCommand;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendZoom;
import com.munger.stereocamera.bluetooth.utility.PreviewSender;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.widget.OrientationCtrl;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.ZoomWidget;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class SlaveFragment extends PreviewFragment
{
	PhotoOrientation orientation;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		int type = 0;
		float zoom = 1.0f;

		if (savedInstanceState != null)
		{
			type = savedInstanceState.getInt("previewType", 0);
			zoom = savedInstanceState.getFloat("zoom", 1.0f);
		}

		PreviewOverlayWidget.Type overlayType = PreviewOverlayWidget.Type.values()[type];

				orientation = getCurrentOrientation();
		if (orientation.isPortait())
			rootView = inflater.inflate(R.layout.fragment_slave, container, false);
		else
			rootView = inflater.inflate(R.layout.fragment_slave_horizontal, container, false);

		super.onCreateView(rootView);

		controls = rootView.findViewById(R.id.controls);
		zoomSlider = rootView.findViewById(R.id.zoom_slider);
		zoomSlider.setListener(new ZoomWidget.Listener() { public void onChange(float value)
		{
			setZoom(value);
		}});
		zoomSlider.set(zoom);
		setZoom(zoom);

		overlayWidget = rootView.findViewById(R.id.previewOverlay);
		overlayWidget.setType(overlayType);

		previewSender = new PreviewSender();

		return rootView;
	}

	private class ZoomListener implements SeekBar.OnSeekBarChangeListener
	{
		private boolean touched = false;

		@Override
		public void onProgressChanged(SeekBar seekBar, int i, boolean b)
		{
			if (touched)
			{
				float value = seekToValue(seekBar);
				setZoom(value);
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

			float value = seekToValue(seekBar);
			slaveComm.sendCommand(new SendZoom(value));
		}
	}

	private float seekToValue(SeekBar bar)
	{
		int idx = bar.getProgress();
		float ret = ((float) idx + 100.0f) / 100.0f;
		return ret;
	}

	private void setSeek(SeekBar bar, float zoom)
	{
		int idx = (int)(zoom * 100.0f - 100.0f);
		idx = Math.max(0, Math.min(idx, bar.getMax()));
		bar.setProgress(idx);
	}

	private void setOverlay(PreviewOverlayWidget.Type type)
	{
		if (type == PreviewOverlayWidget.Type.Ghost)
			previewSender.start();
		else
			previewSender.cancel();

		overlayWidget.setType(type);
	}

	private BluetoothSlaveComm slaveComm;
	private byte[] imgBytes;
	private OrientationCtrl gravityCtrl;

	private ViewGroup controls;
	private ZoomWidget zoomSlider;
	private PreviewOverlayWidget overlayWidget;
	private PreviewSender previewSender;

	private long lastSent = 0;
	private final Object gravityLock = new Object();
	private boolean sending = false;

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
				float zoom = zoomCommand.getZoom();
				SlaveFragment.this.setZoom(zoom);
				zoomSlider.set(zoom);
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

		slaveComm.addListener(BluetoothCommands.SET_OVERLAY, new BluetoothSlaveComm.Listener() { public void onCommand(SlaveCommand command)
		{
			ReceiveOverlay cmd = (ReceiveOverlay) command;
			SlaveFragment.this.setOverlay(cmd.getType());
		}});

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
				cmd.setZoom(zoomSlider.get());

				byte[] data = doFireShutter();
				cmd.setData(data);
			}
		});

		slaveComm.addListener(BluetoothCommands.PING, new BluetoothSlaveComm.Listener()
		{
			@Override
			public void onCommand(SlaveCommand command)
			{
				if (!pingReceived)
					setStatus(Status.LISTENING);

				pingReceived = true;
			}
		});

		slaveComm.addListener(BluetoothCommands.DISCONNECT, new BluetoothSlaveComm.Listener() { public void onCommand(SlaveCommand command)
		{
			((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
		}});

		slaveComm.addListener(BluetoothCommands.SEND_PROCESSED_PHOTO, new BluetoothSlaveComm.Listener() {public void onCommand(SlaveCommand command)
		{
			ReceiveProcessedPhoto cmd = (ReceiveProcessedPhoto) command;
			saveProcessedPhoto(cmd.getData());
		}});

		slaveComm.sendCommand(new SendOrientation(orientation));

		previewSender.setPreviewWidget(previewView);
		previewSender.setSlaveComm(slaveComm);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		previewSender.cancel();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putInt("previewType", overlayWidget.getType().ordinal());
		outState.putFloat("zoom", zoomSlider.get());
	}

	private boolean pingReceived = false;
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

			@Override
			public void onFinished()
			{
				setStatus(Status.READY);
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

	PhotoFiles photoFiles = null;
	Handler handler = null;

	private void saveProcessedPhoto(final byte[] data)
	{
		if (photoFiles == null)
			 photoFiles = new PhotoFiles(getContext());

		photoFiles.checkPermissions(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				saveProcessedPhoto2(data);
			}

			@Override
			public void fail()
			{}
		});
	}

	private void saveProcessedPhoto2(final byte[] data)
	{
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				photoFiles.saveNewFile(data);

				if (handler == null)
					handler = new Handler(Looper.getMainLooper());

				handler.post(new Runnable() { public void run()
				{
					Toast.makeText(getContext(), R.string.new_photo_available, Toast.LENGTH_LONG).show();
				}});
			}

			@Override
			public void fail()
			{}
		});
	}

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();

		if (overlayWidget.getType() == PreviewOverlayWidget.Type.Ghost)
			previewSender.start();
	}




}

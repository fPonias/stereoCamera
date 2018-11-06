package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import com.munger.stereocamera.BuildConfig;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.commands.ConnectionPause;
import com.munger.stereocamera.ip.command.commands.FireShutter;
import com.munger.stereocamera.ip.command.commands.Handshake;
import com.munger.stereocamera.ip.command.commands.ID;
import com.munger.stereocamera.ip.command.commands.LatencyTest;
import com.munger.stereocamera.ip.command.commands.Ping;
import com.munger.stereocamera.ip.command.commands.SendGravity;
import com.munger.stereocamera.ip.command.commands.SendPhoto;
import com.munger.stereocamera.ip.command.commands.SendStatus;
import com.munger.stereocamera.ip.command.commands.SendZoom;
import com.munger.stereocamera.ip.command.commands.SetCaptureQuality;
import com.munger.stereocamera.ip.command.commands.SetFacing;
import com.munger.stereocamera.ip.command.commands.SetOverlay;
import com.munger.stereocamera.ip.command.commands.SetZoom;
import com.munger.stereocamera.ip.command.commands.Version;
import com.munger.stereocamera.ip.utility.PreviewSender;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.widget.OrientationCtrl;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;
import com.munger.stereocamera.widget.ZoomWidget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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

		orientation = MainActivity.getInstance().getCurrentOrientation();
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
			slaveComm.sendCommand(new SendZoom(value), null);
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

	private void checkVersion(int version)
	{
		if (BuildConfig.VERSION_CODE < version)
		{

		}
	}

	private CommCtrl slaveComm;
	private byte[] imgBytes;

	private ViewGroup controls;
	private ZoomWidget zoomSlider;
	private PreviewOverlayWidget overlayWidget;
	private PreviewSender previewSender;

	private long lastSent = 0;
	private final Object gravityLock = new Object();
	private boolean sending = false;
	private PreviewWidget.SHUTTER_TYPE captureQuality = PreviewWidget.SHUTTER_TYPE.PREVIEW;

	@Override
	public void onStart()
	{
		super.onStart();

		slaveComm = MainActivity.getInstance().getCtrl();

		if (slaveComm == null)
		{
			Toast.makeText(getActivity(), R.string.bluetooth_master_communication_failed_error, Toast.LENGTH_LONG).show();
			MainActivity.getInstance().popSubViews();
			return;
		}

		slaveComm.registerListener(Command.Type.SET_ZOOM, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SetZoom zoomCommand = (SetZoom) command;
			float zoom = zoomCommand.zoom;
			SlaveFragment.this.setZoom(zoom);
			zoomSlider.set(zoom);
		}});

		slaveComm.registerListener(Command.Type.SET_FACING, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SetFacing facingCommand = (SetFacing) command;
			SlaveFragment.this.setFacing(facingCommand.facing);
		}});

		slaveComm.registerListener(Command.Type.SET_OVERLAY, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SetOverlay cmd = (SetOverlay) command;
			SlaveFragment.this.setOverlay(cmd.type);
		}});

		slaveComm.registerListener(Command.Type.SEND_VERSION, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			Version cmd = (Version) command;
			cmd.platform = Version.Platform.ANDROID;
			cmd.version = Version.getVersion();
		}});

		slaveComm.registerListener(Command.Type.ID, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			ID cmd = (ID) command;
			cmd.phoneId = MainActivity.getInstance().getPrefs().getId();
		}});

		slaveComm.registerListener(Command.Type.LATENCY_CHECK, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			long now = System.currentTimeMillis();
			doLatencyCheck();
			long then = System.currentTimeMillis();
			long diff = then - now;

			((LatencyTest) command).elapsed = diff;
		}});


		slaveComm.registerListener(Command.Type.SET_CAPTURE_QUALITY, new CommCtrl.Listener() {public void onCommand(Command command)
		{
			SetCaptureQuality qual = (SetCaptureQuality) command;
			captureQuality = qual.quality;
		}});

		slaveComm.registerListener(Command.Type.FIRE_SHUTTER, new CommCtrl.Listener() {
			public void onCommand(Command command) {
				FireShutter cmd = (FireShutter) command;

				cmd.zoom = (zoomSlider.get());

				byte[] data = doFireShutter(captureQuality);
				cmd.data = data;
			}
		});

		slaveComm.registerListener(Command.Type.HANDSHAKE, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			cancelConnection();

			synchronized (pingLock)
			{
				pingReceived = true;
				pingLock.notify();
			}

			setStatus(Status.LISTENING);
		}});

		slaveComm.registerListener(Command.Type.CONNECTION_PAUSE, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			cancelConnection();
		}});

		slaveComm.registerListener(Command.Type.DISCONNECT, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			disconnect();
		}});

		slaveComm.registerListener(Command.Type.SEND_PROCESSED_PHOTO, new CommCtrl.Listener() { public void onCommand(Command command)
		{
			SendPhoto cmd = (SendPhoto) command;
			saveProcessedPhoto(cmd);
		}});

		previewSender.setPreviewWidget(previewView);
		previewSender.setSlaveComm(slaveComm);
	}

	private MainActivity.Listener appListener;
	private boolean previewAlreadyStarted;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.containsKey("previewAlreadyStarted"))
			previewAlreadyStarted = savedInstanceState.getBoolean("previewAlreadyStarted");

		setHasOptionsMenu(true);

		appListener = new MainActivity.Listener()
		{
			@Override
			public void onScreenChanged(boolean isOn)
			{
				if (isOn)
					return;

				cancelConnection();
			}

			@Override
			public void onBluetoothChanged(boolean isConnected, BluetoothDevice device)
			{
				if (!isConnected)
				{
					disconnect();
				}
			}
		};
		MainActivity.getInstance().addListener(appListener);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		MainActivity.getInstance().removeListener(appListener);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		pause();
	}

	public void pause()
	{
		cancelConnection();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		setStatus(Status.RESUMED);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putInt("previewType", overlayWidget.getType().ordinal());
		outState.putFloat("zoom", zoomSlider.get());
	}

	private void cancelConnection()
	{
		pingReceived = false;
		previewSender.cancel();
		stopPreview();

		if (status == Status.READY || status == Status.BUSY)
			slaveComm.sendCommand(new ConnectionPause(), null);

		setStatus(Status.CREATED);
	}

	private void disconnect()
	{
		cancelConnection();
		slaveComm.cleanUpConnections();
		MainActivity.getInstance().popSubViews();
	}

	private static final long PING_TIMEOUT = 1000;
	private final Object pingLock = new Object();
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

	private byte[] doFireShutter(PreviewWidget.SHUTTER_TYPE type)
	{
		final Object lock = new Object();
		imgBytes = null;

		fireShutter(type, new PreviewWidget.ImageListener()
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
			Log.d("stereoCamera", "sending " + status.name() + " status");
			slaveComm.sendCommand(new SendStatus(status), null);
		}
	}

	PhotoFiles photoFiles = null;
	Handler handler = null;

	private void saveProcessedPhoto(final SendPhoto cmd)
	{
		if (photoFiles == null)
			 photoFiles = new PhotoFiles(getContext());

		final File tmpFile = cmd.file;
		cmd.file = null;  //set file to null so we don't send it back with the response

		photoFiles.checkPermissions(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				saveProcessedPhoto2(tmpFile);
			}

			@Override
			public void fail()
			{}
		});
	}

	private void saveProcessedPhoto2(final File tmpFile)
	{
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				FileOutputStream fos = null;
				FileInputStream fis = null;

				try
				{
					String path = photoFiles.getNewFilePath();
					String tmpPath = tmpFile.getPath();
					byte[] buffer = new byte[1024];
					int read = 1;

					fos = new FileOutputStream(path);
					fis = new FileInputStream(tmpPath);
					while (read > 0)
					{
						read = fis.read(buffer);

						if (read > 0)
							fos.write(buffer, 0, read);
					}
					fos.flush();
				}
				catch(IOException e){

				}
				finally{
					try{
						if (fos != null)
							fos.close();
					}
					catch(IOException e){}

					try{
						if (fis != null)
							fis.close();
					}
					catch (IOException e){}
				}

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

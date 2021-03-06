package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.munger.stereocamera.BuildConfig;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.commands.ConnectionPause;
import com.munger.stereocamera.ip.command.commands.FireShutter;
import com.munger.stereocamera.ip.command.commands.LatencyTest;
import com.munger.stereocamera.ip.command.commands.SendPhoto;
import com.munger.stereocamera.ip.command.commands.SendStatus;
import com.munger.stereocamera.ip.command.commands.SetCaptureQuality;
import com.munger.stereocamera.ip.command.commands.SetFacing;
import com.munger.stereocamera.ip.command.commands.SetOverlay;
import com.munger.stereocamera.ip.command.commands.SetZoom;
import com.munger.stereocamera.ip.command.commands.Version;
import com.munger.stereocamera.ip.utility.PreviewSender;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.data.Client;
import com.munger.stereocamera.utility.data.ClientViewModel;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;
import com.munger.stereocamera.widget.ThumbnailWidget;
import com.munger.stereocamera.widget.ZoomWidget;

import java.io.File;

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
		if (MainActivity.getInstance().isPortrait())
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

		thumbnailWidget = rootView.findViewById(R.id.thumbnail);
		thumbnailWidget.setOnClickListener(new View.OnClickListener() { public void onClick(View v)
		{
			openThumbnail();
		}});

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

			//zoom data shared on shutter trigger
			//float value = seekToValue(seekBar);
			//slaveComm.sendCommand(new SendZoom(value), null);
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
	private ThumbnailWidget thumbnailWidget;

	private long lastSent = 0;
	private final Object gravityLock = new Object();
	private boolean sending = false;
	private PreviewWidget.ShutterType captureQuality = PreviewWidget.ShutterType.PREVIEW;

	@Override
	public void onStart()
	{
		super.onStart();

		slaveComm = MyApplication.getInstance().getCtrl();

		if (slaveComm == null)
		{
			Toast.makeText(getContext(), R.string.bluetooth_master_communication_failed_error, Toast.LENGTH_LONG).show();
			MainActivity.getInstance().popSubViews();
			return;
		}

		slaveComm.registerListener(Command.Type.SET_ZOOM, command ->
		{
			SetZoom zoomCommand = (SetZoom) command;
			float zoom = zoomCommand.remoteZoom;
			SlaveFragment.this.setZoom(zoom);
			zoomSlider.set(zoom);

			cameraPair.local.zoom = zoom;
			clientViewModel.update(cameraPair.local);
			cameraPair.remote.zoom = zoomCommand.localZoom;
			clientViewModel.update(cameraPair.remote);
		});

		slaveComm.registerListener(Command.Type.SET_FACING, command ->
		{
			SetFacing facingCommand = (SetFacing) command;
			SlaveFragment.this.setFacing(facingCommand.facing);

			cameraPair = clientViewModel.getCameras(client.id, facingCommand.facing);
			client.isFacing = facingCommand.facing;
			clientViewModel.update(client);
		});

		slaveComm.registerListener(Command.Type.SET_OVERLAY, command ->
		{
			SetOverlay cmd = (SetOverlay) command;
			SlaveFragment.this.setOverlay(cmd.type);

			client.overlay = cmd.type;
			clientViewModel.update(client);

			setStatus(Status.READY);
		});

		slaveComm.registerListener(Command.Type.SEND_VERSION, command ->
		{
			Version cmd = (Version) command;
			cmd.platform = Version.Platform.ANDROID;
			cmd.version = Version.getVersion();
		});

		slaveComm.registerListener(Command.Type.ID, command -> { });

		slaveComm.registerListener(Command.Type.LATENCY_CHECK, command ->
		{
			long now = System.currentTimeMillis();
			doLatencyCheck();
			long then = System.currentTimeMillis();
			long diff = then - now;

			((LatencyTest) command).elapsed = diff;
		});


		slaveComm.registerListener(Command.Type.SET_CAPTURE_QUALITY, command ->
		{
			SetCaptureQuality qual = (SetCaptureQuality) command;
			captureQuality = qual.quality;

			client.resolution = captureQuality;
			clientViewModel.update(client);
		});

		slaveComm.registerListener(Command.Type.FIRE_SHUTTER, command ->
		{
			FireShutter cmd = (FireShutter) command;

			cameraPair.remote.zoom = cmd.zoom;
			clientViewModel.update(cameraPair.remote);

			cmd.zoom = (zoomSlider.get());
			cmd.orientation = orientation;

			setStatus(Status.PROCESSING);
			setLoadingMessage("Taking picture");

			byte[] data = doFireShutter(captureQuality);
			cmd.data = data;
		});

		slaveComm.registerListener(Command.Type.HANDSHAKE, command ->
		{
			cancelConnection();

			synchronized (pingLock)
			{
				pingReceived = true;
				pingLock.notify();
			}

			setStatus(Status.LISTENING);
		});

		slaveComm.registerListener(Command.Type.CONNECTION_PAUSE, command -> cancelConnection());

		slaveComm.registerListener(Command.Type.DISCONNECT, command -> disconnect());

		slaveComm.registerListener(Command.Type.SEND_PROCESSED_PHOTO, command ->
		{
			SendPhoto cmd = (SendPhoto) command;
			saveProcessedPhoto(cmd);
			showLoading(false);
		});

		previewSender.setPreviewWidget(previewView);
		previewSender.setSlaveComm(slaveComm);
	}

	private MainActivity.Listener appListener;
	private boolean previewAlreadyStarted;

	private ClientViewModel.CameraDataPair cameraPair;
	private Client client;
	private ClientViewModel clientViewModel;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		new Thread(() -> {
			clientViewModel = MainActivity.getInstance().getClientViewModel();
			client = clientViewModel.getCurrentClient();
			cameraPair = clientViewModel.getCameras(client.id, false);
		}).start();

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

	private void openThumbnail()
	{
		MainActivity.getInstance().startGalleryView();
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

	private byte[] doFireShutter(PreviewWidget.ShutterType type)
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
				setLoadingMessage("Waiting for image");
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
			 photoFiles = PhotoFiles.Factory.get();

		final File tmpFile = cmd.file;
		cmd.file = null;  //set file to null so we don't send it back with the response

		saveProcessedPhoto2(tmpFile);
	}

	private void saveProcessedPhoto2(final File tmpFile)
	{
		photoFiles.saveFile(tmpFile);

		if (handler == null)
			handler = new Handler(Looper.getMainLooper());

		handler.post(new Runnable() { public void run()
		{
			Toast.makeText(getContext(), R.string.new_photo_available, Toast.LENGTH_LONG).show();

		}});
	}

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();

		if (overlayWidget.getType() == PreviewOverlayWidget.Type.Ghost)
			previewSender.start();
	}
}

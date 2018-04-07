package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.bluetooth.command.master.commands.ConnectionPause;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Ping;
import com.munger.stereocamera.bluetooth.command.master.commands.SetFacing;
import com.munger.stereocamera.bluetooth.command.master.commands.SetOverlay;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.bluetooth.command.master.commands.SetZoom;
import com.munger.stereocamera.bluetooth.utility.CalculateSync;
import com.munger.stereocamera.bluetooth.utility.FireShutter;
import com.munger.stereocamera.bluetooth.utility.RemoteState;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.widget.OrientationCtrl;
import com.munger.stereocamera.widget.OrientationWidget;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.SlavePreviewOverlayWidget;
import com.munger.stereocamera.widget.ThumbnailWidget;
import com.munger.stereocamera.widget.ZoomWidget;

public class MasterFragment extends PreviewFragment
{
	private ImageButton clickButton;
	private OrientationWidget verticalIndicator;
	private OrientationWidget horizontalIndicator;
	private ViewGroup controls;
	PreviewOverlayWidget overlayWidget;
	private ThumbnailWidget thumbnailWidget;
	SlavePreviewOverlayWidget slavePreview;
	private ZoomWidget zoomSlider;

	BluetoothMasterComm masterComm;
	RemoteState remoteState;

	private long shutterDelay;
	private PhotoOrientation orientation;

	private MainActivity.Listener appListener;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		orientation = getCurrentOrientation();
		if (orientation.isPortait())
			rootView = inflater.inflate(R.layout.fragment_master, container, false);
		else
			rootView = inflater.inflate(R.layout.fragment_master_horizontal, container, false);

		super.onCreateView(rootView);

		controls = rootView.findViewById(R.id.controls);

		clickButton = rootView.findViewById(R.id.shutter);
		verticalIndicator = rootView.findViewById(R.id.vert_level);
		horizontalIndicator = rootView.findViewById(R.id.horiz_level);

		zoomSlider = rootView.findViewById(R.id.zoom_slider);
		zoomSlider.setListener(new ZoomWidget.Listener() {public void onChange(float value)
		{
			setZoom(value);
		}});

		overlayWidget = rootView.findViewById(R.id.previewOverlay);
		updateOverlayFromPrefs();

		clickButton.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			doShutter();
		}});

		startLocalGravity();

		Preferences prefs = MainActivity.getInstance().getPrefs();
		previewView.setOrientation(orientation);

		slavePreview = rootView.findViewById(R.id.slavePreview);

		thumbnailWidget = rootView.findViewById(R.id.thumbnail);
		thumbnailWidget.setOnClickListener(new View.OnClickListener() { public void onClick(View v)
		{
			openThumbnail();
		}});

		setStatus(Status.CREATED);

		return rootView;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		appListener = new MainActivity.Listener()
		{
			@Override
			public void onScreenChanged(boolean isOn)
			{
				if (isOn)
					return;

				pauseConnection();
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

		masterHandshake = new MasterHandshake(this);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		MainActivity.getInstance().removeListener(appListener);
	}

	private MenuItem flipItem;
	private MenuItem swapItem;
	private MenuItem debugItem;
	private MenuItem galleryItem;
	private MenuItem prefsItem;
	private MenuItem helpItem;


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.master_menu, menu);

		flipItem = menu.findItem(R.id.flip);
		swapItem = menu.findItem(R.id.swap);
		debugItem = menu.findItem(R.id.test);
		galleryItem = menu.findItem(R.id.gallery);
		prefsItem = menu.findItem(R.id.prefs);
		helpItem = menu.findItem(R.id.help);

		updateHandPhoneButton();

		flipItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			onFlip();

			return true;
		}});

		swapItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			Preferences prefs = MainActivity.getInstance().getPrefs();
			boolean val = !prefs.getIsOnLeft();
			prefs.setIsOnLeft(val);

			updateHandPhoneButton();

			return true;
		}});

		debugItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			PhotoProcessor proc = new PhotoProcessor(getContext(), PhotoProcessor.CompositeImageType.SPLIT);
			proc.testOldData();

			return true;
		}});
		debugItem.setVisible(false);

		galleryItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			openThumbnail();
			return true;
		}});

		prefsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			((MainActivity) getActivity()).openSettings();
			return true;
		}});

		helpItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem item)
		{
			((MainActivity) getActivity()).openHelp();
			return false;
		}});
	}

	private void onFlip()
	{
		setStatus(Status.BUSY);
		boolean isFacing = MainActivity.getInstance().getPrefs().getIsFacing();
		isFacing = !isFacing;
		setCamera(isFacing, new SetCameraListener()
		{
			@Override
			public void done()
			{
				setStatus(Status.READY);
			}

			@Override
			public void fail()
			{
				setStatus(Status.READY);
			}
		});
	}

	interface SetCameraListener
	{
		void done();
		void fail();
	}

	void setCamera(boolean isFacing, final SetCameraListener listener)
	{
		setFacing(isFacing);
		MainActivity.getInstance().getPrefs().setIsFacing(isFacing);

		masterComm.runCommand(new SetFacing(isFacing), new BluetoothMasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				setCamera2(listener);
			}
		});
	}

	private void setCamera2(final SetCameraListener listener)
	{
		String cameraId = getCameraId();
		Preferences prefs = MainActivity.getInstance().getPrefs();
		float localZoom = prefs.getLocalZoom(cameraId);
		setZoom(localZoom);

		float zoom = prefs.getRemoteZoom(cameraId);
		masterComm.runCommand(new SetZoom(zoom), new BluetoothMasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				listener.done();
			}
		});
	}

	@Override
	protected void setZoom(float zoom)
	{
		super.setZoom(zoom);
		zoomSlider.set(zoom);
	}

	public float getZoom()
	{
		return zoomSlider.get();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		pause();
	}

	public void pause()
	{
		resumed = false;
		pauseConnection();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		resumed = true;

		if (overlayWidget == null)
			return;

		updateOverlayFromPrefs();
		resumeConnection();
	}

	private void pauseConnection()
	{
		if (slavePreview != null)
		{
			slavePreview.cancel();
			previewView.stopPreview();
		}

		if ((status == Status.READY || status == Status.BUSY) && (remoteState.status == Status.READY || remoteState.status == Status.BUSY))
			masterComm.runCommand(new ConnectionPause(), null);

		setStatus(Status.CREATED);
	}

	private void disconnect()
	{
		pauseConnection();
		MainActivity.getInstance().cleanUpConnections();
		MainActivity.getInstance().popSubViews();
	}

	private void resumeConnection()
	{
		handshake();
	}

	private void updateOverlayFromPrefs()
	{
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		String overlayType = sharedPref.getString("pref_overlay", "1");
		int overlayIdx = Integer.parseInt(overlayType);
		PreviewOverlayWidget.Type otype = PreviewOverlayWidget.Type.values()[overlayIdx];
		setOverlayType(otype);
	}

	private void setOverlayType(PreviewOverlayWidget.Type type)
	{
		if (slavePreview != null && status == Status.READY)
		{
			if (type == PreviewOverlayWidget.Type.Ghost)
				slavePreview.start();
			else
				slavePreview.cancel();
		}

		overlayWidget.setType(type);

		if (remoteState != null && remoteState.status == Status.READY && masterComm != null)
			masterComm.runCommand(new SetOverlay(type), null);
	}

	@Override
	protected void setStatus(Status status)
	{
		super.setStatus(status);

		if (slavePreview != null)
		{
			if (status == Status.READY && overlayWidget.getType() == PreviewOverlayWidget.Type.Ghost && !slavePreview.isRunning())
				slavePreview.start();
			else if (status != Status.READY && slavePreview.isRunning())
				slavePreview.cancel();
		}
	}

	private void updateHandPhoneButton()
	{
		Preferences prefs = MainActivity.getInstance().getPrefs();
		boolean val = prefs.getIsOnLeft();

		if (val)
			swapItem.setIcon(R.drawable.hand_phone_white);
		else
			swapItem.setIcon(R.drawable.hand_phone_white_right);
	}

	private OrientationCtrl orientationCtrl;
	private void startLocalGravity()
	{
		orientationCtrl = new OrientationCtrl();
		orientationCtrl.setChangeListener(new OrientationCtrl.ChangeListener()
		{
			@Override
			public void change(float[] values)
			{
				double az = OrientationCtrl.verticalOrientation(values[0], values[1], values[2]);
				verticalIndicator.setRotation((float) az - 90.0f);

				double ax = OrientationCtrl.horizontalOrientation(orientation, values[0], values[1], values[2]);
				horizontalIndicator.setRotation((float) ax);
			}
		});
		orientationCtrl.start();
	}

	private void doShutter()
	{
		setStatus(Status.BUSY);

		new FireShutter(MasterFragment.this).execute(shutterDelay, new FireShutter.Listener()
		{
			@Override
			public void onProcessing()
			{
				setStatus(Status.PROCESSING);
			}

			@Override
			public void done()
			{
				setStatus(Status.READY);
			}

			@Override
			public void fail()
			{
				setStatus(Status.READY);
			}
		});
	}

	private void doSync()
	{
		setStatus(Status.BUSY);
		CalculateSync sync = new CalculateSync(this, new CalculateSync.Listener()
		{
			@Override
			public void result(long localDelay)
			{
				MainActivity.getInstance().getPrefs().setShutterDelay(getCameraId(), localDelay);
				setStatus(Status.READY);
			}

			@Override
			public void fail()
			{
				setStatus(Status.READY);
			}
		});
		sync.execute();
	}

	private void openThumbnail()
	{
		MainActivity.getInstance().startThumbnailView();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		masterComm = MainActivity.getInstance().getBtCtrl().getMaster().getComm();
		handler = new Handler(Looper.getMainLooper());

		remoteState = MainActivity.getInstance().getBtCtrl().getMaster().getRemoteState();
		remoteState.addListener(remoteListener);
		remoteState.start();

	}

	@Override
	public void onStop()
	{
		super.onStop();

		remoteState.removeListener(remoteListener);
	}

	private Handler handler;
	private RemoteState.Listener remoteListener = new RemoteState.Listener()
	{
		@Override
		public void onZoom(float zoom)
		{
			MainActivity.getInstance().getPrefs().setRemoteZoom(getCameraId(), zoom);
		}

		@Override
		public void onGravity(ReceiveGravity.Gravity value)
		{
			final double az = OrientationCtrl.verticalOrientation(value.x, value.y, value.z);
			final double ax = OrientationCtrl.horizontalOrientation(remoteState.orientation, value.x, value.y, value.z);

			handler.post(new Runnable() {public void run()
			{
				verticalIndicator.setRotation2((float) az - 90.0f);
				horizontalIndicator.setRotation2((float) ax);
			}});
		}

		@Override
		public void onConnectionPause()
		{
			pauseConnection();
		}

		@Override
		public void onDisconnect()
		{
			disconnect();
		}

		@Override
		public void onPreviewFrame(byte[] data, float zoom)
		{
			slavePreview.render(data, zoom);
		}

		@Override
		public void onStatus(Status status)
		{
			if (status == Status.RESUMED && resumed)
			{
				if (!(MasterFragment.this.status == Status.CREATED || MasterFragment.this.status == Status.LISTENING))
				{
					handshake();
				}
				else
				{
					pauseConnection();
					handshake();
				}
			}
		}
	};

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();

		startPreview();
	}

	private boolean resumed = false;

	private MasterHandshake masterHandshake;

	private void handshake()
	{
		masterHandshake.start(new MasterHandshake.Listener()
		{
			@Override
			public void success()
			{
				handshakeSuccess();
			}

			@Override
			public void fail()
			{
				handshakeFail();
			}
		});
	}

	private void handshakeSuccess()
	{
		Log.d(getTag(), "slave ready");
		setStatus(PreviewFragment.Status.READY);

		Context c = MainActivity.getInstance();
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
		boolean runSync = sharedPref.getBoolean("pref_sync", false);

		if (runSync)
			doSync();

		sharedPref.edit().putBoolean("pref_sync", false).apply();
	}

	private void handshakeFail()
	{
		handler.post(new Runnable() {public void run()
		{
			Log.d(getTag(), "slave setup failed");
			Toast.makeText(MainActivity.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
			pauseConnection();
			MainActivity.getInstance().popSubViews();
		}});
	}
}

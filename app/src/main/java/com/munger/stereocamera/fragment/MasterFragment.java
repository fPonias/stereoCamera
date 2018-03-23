package com.munger.stereocamera.fragment;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
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

import java.util.Set;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class MasterFragment extends PreviewFragment
{
	private ImageButton clickButton;
	private OrientationWidget verticalIndicator;
	private OrientationWidget horizontalIndicator;
	private ViewGroup controls;
	private PreviewOverlayWidget overlayWidget;
	private ThumbnailWidget thumbnailWidget;
	private SlavePreviewOverlayWidget slavePreview;
	private ZoomWidget zoomSlider;

	private BluetoothMasterComm masterComm;
	private RemoteState remoteState;

	private long shutterDelay;
	private PhotoOrientation orientation;

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

		Preferences prefs = MyApplication.getInstance().getPrefs();
		previewView.setOrientation(orientation);
		previewView.setAndStartCamera(prefs.getIsFacing());

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

		if (savedInstanceState != null && savedInstanceState.containsKey("previewAlreadyStarted"))
			previewAlreadyStarted = savedInstanceState.getBoolean("previewAlreadyStarted");

		setHasOptionsMenu(true);
	}

	private MenuItem flipItem;
	private MenuItem swapItem;
	private MenuItem debugItem;
	private MenuItem galleryItem;
	private MenuItem prefsItem;


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.master_menu, menu);

		flipItem = menu.findItem(R.id.flip);
		swapItem = menu.findItem(R.id.swap);
		debugItem = menu.findItem(R.id.test);
		galleryItem = menu.findItem(R.id.gallery);
		prefsItem = menu.findItem(R.id.prefs);

		updateHandPhoneButton();

		flipItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			onFlip();

			return true;
		}});

		swapItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			Preferences prefs = MyApplication.getInstance().getPrefs();
			boolean val = !prefs.getIsOnLeft();
			prefs.setIsOnLeft(val);

			updateHandPhoneButton();

			return true;
		}});

		debugItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			PhotoProcessor proc = new PhotoProcessor(getContext());
			proc.testOldData();

			return true;
		}});
		debugItem.setVisible(true);

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
	}

	private void onFlip()
	{
		setStatus(Status.BUSY);
		boolean isFacing = MyApplication.getInstance().getPrefs().getIsFacing();
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

	private interface SetCameraListener
	{
		void done();
		void fail();
	}

	private void setCamera(boolean isFacing, final SetCameraListener listener)
	{
		setFacing(isFacing);
		MyApplication.getInstance().getPrefs().setIsFacing(isFacing);

		masterComm.runCommand(new SetFacing(isFacing, new SetFacing.Listener()
		{
			@Override
			public void done()
			{
				setCamera2(listener);
			}

			@Override
			public void fail()
			{
				listener.fail();
			}
		}));
	}

	private void setCamera2(final SetCameraListener listener)
	{
		String cameraId = getCameraId();
		Preferences prefs = MyApplication.getInstance().getPrefs();
		float localZoom = prefs.getLocalZoom(cameraId);
		setZoom(localZoom);

		float zoom = prefs.getRemoteZoom(cameraId);
		masterComm.runCommand(new SetZoom(zoom, new SetZoom.Listener()
		{
			@Override
			public void done()
			{
				listener.done();
			}

			@Override
			public void fail()
			{
				listener.fail();
			}
		}));
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
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean("previewAlreadyStarted", previewAlreadyStarted);

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		slavePreview.cancel();
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (overlayWidget == null)
			return;

		updateOverlayFromPrefs();
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
			masterComm.runCommand(new SetOverlay(type));
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
		Preferences prefs = MyApplication.getInstance().getPrefs();
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
				MyApplication.getInstance().getPrefs().setShutterDelay(getCameraId(), localDelay);
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
		BaseActivity act = MyApplication.getInstance().getCurrentActivity();
		if (act instanceof MainActivity)
			((MainActivity) act).startThumbnailView();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
		remoteState = MyApplication.getInstance().getBtCtrl().getMaster().getRemoteState();
		handler = new Handler(Looper.getMainLooper());
		remoteState.addListener(remoteListener);
		remoteState.start();

	}

	private Handler handler;
	private RemoteState.Listener remoteListener = new RemoteState.Listener()
	{
		@Override
		public void onZoom(float zoom)
		{
			MyApplication.getInstance().getPrefs().setRemoteZoom(getCameraId(), zoom);
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
		public void onDisconnect()
		{
			((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
		}

		@Override
		public void onPreviewFrame(byte[] data, float zoom)
		{
			slavePreview.render(data, zoom);
		}
	};

	private boolean previewAlreadyStarted = false;

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();

		if (previewAlreadyStarted)
			return;
		else
			previewAlreadyStarted = true;


		setStatus(Status.LISTENING);

		onPreviewStarted1();
	}

	private void onPreviewStarted1()
	{
		Log.d(getTag(), "pinging slave phone");
		masterComm.runCommand(new Ping(new Ping.Listener()
		{
			@Override
			public void pong(long diff)
			{
				Log.d(getTag(), "pinged slave phone for " + diff + " ms");
				onPreviewStarted2();
			}

			@Override
			public void fail()
			{
				onSetupFailed();
			}
		}));
	}

	private void onPreviewStarted2()
	{
		remoteState.waitOnStatusAsync(Status.LISTENING,3000, new RemoteState.ReadyListener()
		{
			@Override
			public void done()
			{
				onPreviewStarted3();
			}

			@Override
			public void fail()
			{
				onSetupFailed();
			}
		});
	}

	private void onPreviewStarted3()
	{
		boolean isFacing = MyApplication.getInstance().getPrefs().getIsFacing();
		setCamera(isFacing, new SetCameraListener()
		{
			@Override
			public void done()
			{
				onPreviewStarted4();
			}

			@Override
			public void fail()
			{
				onSetupFailed();
			}
		});
	}

	private void onPreviewStarted4()
	{
		PreviewOverlayWidget.Type type = overlayWidget.getType();
		masterComm.runCommand(new SetOverlay(type));

		remoteState.waitOnStatusAsync(Status.READY, 6000, new RemoteState.ReadyListener()
		{
			@Override
			public void done()
			{
				Log.d(getTag(), "slave ready");
				setStatus(Status.READY);

				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
				boolean runSync = sharedPref.getBoolean("pref_sync", false);

				if (runSync)
					doSync();

				sharedPref.edit().putBoolean("pref_sync", false).apply();
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave ready timed out");
				onSetupFailed();
			}
		});
	}

	private void onSetupFailed()
	{
		handler.post(new Runnable() {public void run()
		{
			Log.d(getTag(), "slave setup failed");
			Toast.makeText(MyApplication.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
			((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
		}});
	}


}

package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.commands.ConnectionPause;
import com.munger.stereocamera.ip.command.commands.SendGravity;
import com.munger.stereocamera.ip.command.commands.SetFacing;
import com.munger.stereocamera.ip.command.commands.SetOverlay;
import com.munger.stereocamera.ip.command.commands.SetZoom;
import com.munger.stereocamera.ip.utility.CalculateSync;
import com.munger.stereocamera.ip.utility.FireShutter;
import com.munger.stereocamera.ip.utility.RemoteState;
import com.munger.stereocamera.service.ImagePair;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.utility.SingleThreadedExecutor;
import com.munger.stereocamera.utility.data.Client;
import com.munger.stereocamera.utility.data.ClientViewModel;
import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;
import com.munger.stereocamera.widget.SlavePreviewOverlayWidget;
import com.munger.stereocamera.widget.ThumbnailWidget;
import com.munger.stereocamera.widget.ZoomWidget;

public class MasterFragment extends PreviewFragment
{
	private ImageButton clickButton;
	private ImageButton swapButton;
	PreviewOverlayWidget overlayWidget;
	private ThumbnailWidget thumbnailWidget;
	SlavePreviewOverlayWidget slavePreview;
	private ZoomWidget zoomSlider;

	CommCtrl masterComm;
	RemoteState remoteState;

	private long shutterDelay;
	private PhotoOrientation orientation;
	private FireShutter fireShutter;

	private SingleThreadedExecutor viewModelExec;

	private MainActivity.Listener appListener;

	public MasterFragment() {
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		orientation = MainActivity.getInstance().getCurrentOrientation();
		if (orientation.isPortait())
			rootView = inflater.inflate(R.layout.fragment_master, container, false);
		else
			rootView = inflater.inflate(R.layout.fragment_master_horizontal, container, false);

		super.onCreateView(rootView);

		clickButton = rootView.findViewById(R.id.shutter);
		updateShutterButton();

		swapButton = rootView.findViewById(R.id.hand);

		zoomSlider = rootView.findViewById(R.id.zoom_slider);
		zoomSlider.setListener(this::setZoom);

		overlayWidget = rootView.findViewById(R.id.previewOverlay);
		updateOverlayFromPrefs();

		clickButton.setOnClickListener(view -> doShutter());

		swapButton.setOnClickListener(view -> swapHand());

		thumbnailWidget = rootView.findViewById(R.id.thumbnail);
		thumbnailWidget.setOnClickListener(v -> openThumbnail());

		if (orientation.isPortait())
			updateHandPhoneButtonVertical();
		else
			updateHandPhoneButtonHorizontal();

		//startLocalGravity();

		previewView.setOrientation(orientation);

		slavePreview = rootView.findViewById(R.id.slavePreview);

		setStatus(Status.CREATED);

		return rootView;
	}

	private ClientViewModel.CameraDataPair cameraPair;
	private Client client;
	private ClientViewModel clientViewModel;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		viewModelExec = new SingleThreadedExecutor("SyncTrigger");
		viewModelExec.execute(() ->
		{
			clientViewModel = MainActivity.getInstance().getClientViewModel();
			client = clientViewModel.getCurrentClient();
			cameraPair = clientViewModel.getCameras(client.id, client.isFacing);
		});

		fireShutter = new FireShutter(this);
		setHasOptionsMenu(true);
		shutterDelay = 0;

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
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		MainActivity.getInstance().removeListener(appListener);
		fireShutter.cleanUp();
		viewModelExec.stop();
	}


	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.master_menu, menu);

		MenuItem flipItem = menu.findItem(R.id.flip);
		MenuItem debugItem = menu.findItem(R.id.test);
		MenuItem galleryItem = menu.findItem(R.id.gallery);
		MenuItem prefsItem = menu.findItem(R.id.prefs);
		MenuItem helpItem = menu.findItem(R.id.help);

		flipItem.setOnMenuItemClickListener(menuItem ->
		{
			onFlip();

			return true;
		});

		debugItem.setOnMenuItemClickListener(menuItem ->
		{
			PhotoProcessor proc = new PhotoProcessor(MainActivity.getInstance());
			proc.testOldData();

			return true;
		});
		debugItem.setVisible(false);

		galleryItem.setOnMenuItemClickListener(menuItem ->
		{
			openThumbnail();
			return true;
		});

		prefsItem.setOnMenuItemClickListener(menuItem ->
		{
			MainActivity.getInstance().openSettings();
			return true;
		});

		helpItem.setOnMenuItemClickListener(item ->
		{
			MainActivity.getInstance().openHelp();
			return false;
		});
	}

	private void onFlip()
	{
		setStatus(Status.BUSY);
		final boolean isFacing = !client.isFacing;

		viewModelExec.execute(() ->
		{
			client.isFacing = isFacing;
			clientViewModel.update(client);
		});

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
		if (cameraPair.local.isFacing != isFacing)
		{
			viewModelExec.execute(() ->
			{

				cameraPair = clientViewModel.getCameras(client.id, isFacing);
				setZoom(cameraPair.local.zoom);

				masterComm.sendCommand(new SetFacing(isFacing), (success, command, originalCmd) ->
				{});
				masterComm.sendCommand(new SetZoom(cameraPair.local.zoom, cameraPair.remote.zoom), (success, command, originalCmd) ->
				{});
			});
		}

		setFacing(isFacing);
		setZoom(cameraPair.local.zoom);
		listener.done();
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

	public boolean getIsOnLeft()
	{
		return cameraPair.local.isLeft;
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
			slavePreview.cancel();

		if (previewView != null)
			previewView.stopPreview();

		if ((status == Status.READY || status == Status.BUSY) && (remoteState.status == Status.READY || remoteState.status == Status.BUSY))
			masterComm.sendCommand(new ConnectionPause(), null);

		setStatus(Status.CREATED);
	}

	public void disconnect()
	{
		pauseConnection();
		masterComm.cleanUpConnections();
		MainActivity.getInstance().popSubViews();
	}

	private void resumeConnection()
	{
		viewModelExec.execute(() ->
		{
			try {Thread.sleep(1500);} catch(InterruptedException ignored){}
			handshake();
		});
	}

	private void updateOverlayFromPrefs()
	{
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
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
			masterComm.sendCommand(new SetOverlay(type), null);
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

	private void swapHand()
	{
		boolean val = cameraPair.local.isLeft;
		val = !val;

		cameraPair.local.isLeft = val;
		cameraPair.remote.isLeft = !val;

		viewModelExec.execute(() ->
		{
			clientViewModel.update(cameraPair.local);
			clientViewModel.update(cameraPair.remote);
		});

		if (orientation.isPortait())
			updateHandPhoneButtonVertical();
		else
			updateHandPhoneButtonHorizontal();
	}

	private void updateHandPhoneButtonVertical()
	{
		boolean val = cameraPair.local.isLeft;

		if (val)
		{
			swapButton.setImageResource(R.drawable.hand_phone_white);

			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) swapButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			swapButton.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) clickButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			clickButton.setLayoutParams(lp);
		}
		else
		{
			swapButton.setImageResource(R.drawable.hand_phone_white_right);

			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) swapButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			swapButton.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) clickButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			clickButton.setLayoutParams(lp);
		}
	}

	private void updateHandPhoneButtonHorizontal()
	{
		boolean val = cameraPair.local.isLeft;

		if (!val)
		{
			swapButton.setImageResource(R.drawable.hand_phone_white);

			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) swapButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			swapButton.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) clickButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			clickButton.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) previewContainer.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			previewContainer.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) thumbnailWidget.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			thumbnailWidget.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) zoomSlider.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			lp.addRule(RelativeLayout.START_OF, R.id.previewContainer);
			lp.removeRule(RelativeLayout.END_OF);
			zoomSlider.setLayoutParams(lp);
		}
		else
		{
			swapButton.setImageResource(R.drawable.hand_phone_white_right);

			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) swapButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			swapButton.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) clickButton.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			clickButton.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) previewContainer.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_START);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
			previewContainer.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) thumbnailWidget.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			thumbnailWidget.setLayoutParams(lp);

			lp = (RelativeLayout.LayoutParams) zoomSlider.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_END);
			lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
			lp.addRule(RelativeLayout.END_OF, R.id.previewContainer);
			lp.removeRule(RelativeLayout.START_OF);
			zoomSlider.setLayoutParams(lp);
		}
	}

	private void updateShutterButton()
	{
		boolean val = cameraPair.local.isLeft;

		orientation = MainActivity.getInstance().getCurrentOrientation();
		if (!orientation.isPortait())
		{
			RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) clickButton.getLayoutParams();
			layout.removeRule(RelativeLayout.ALIGN_PARENT_END);
			layout.removeRule(RelativeLayout.ALIGN_PARENT_START);
			if (val)
			{
				layout.addRule(RelativeLayout.ALIGN_PARENT_START);
			}
			else
			{
				layout.addRule(RelativeLayout.ALIGN_PARENT_END);
			}

			clickButton.setLayoutParams(layout);
		}
	}

	/*
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
	}*/

	private void doShutter()
	{
		setStatus(Status.BUSY);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
		String captureTypeStr = sharedPref.getString("pref_capture", "preview");
		PreviewWidget.ShutterType captureType;

		switch(captureTypeStr)
		{
			case "hi cam":
				captureType = PreviewWidget.ShutterType.HI_RES;
				break;
			case "lo cam":
				captureType = PreviewWidget.ShutterType.LO_RES;
				break;
			default:
				captureType = PreviewWidget.ShutterType.PREVIEW;
				break;
		}

		fireShutter.execute(shutterDelay, captureType, new FireShutter.Listener()
		{
			@Override
			public void remoteReceived(final ImagePair.ImageArg arg)
			{
				viewModelExec.execute(() -> {
					cameraPair.remote.zoom = arg.zoom;
					clientViewModel.update(cameraPair.remote);
				});
			}

			@Override
			public void localReceived(final ImagePair.ImageArg arg)
			{
				viewModelExec.execute(() ->
				{
					cameraPair.local.zoom = arg.zoom;
					clientViewModel.update(cameraPair.local);
				});
			}

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
				if (localDelay != client.delay)
				{
					client.delay = localDelay;
					clientViewModel.update(client);
				}

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
		MainActivity.getInstance().startGalleryView();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		masterComm = MyApplication.getInstance().getCtrl();
		handler = new Handler(Looper.getMainLooper());

		remoteState = masterComm.getRemoteState();
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
	private final RemoteState.Listener remoteListener = new RemoteState.Listener()
	{
		@Override
		public void onZoom(float zoom)
		{
			if (cameraPair.remote.zoom != zoom)
			{
				cameraPair.remote.zoom = zoom;
				clientViewModel.update(cameraPair.remote);
			}
		}

		@Override
		public void onGravity(SendGravity.Gravity value)
		{
			/*final double az = OrientationCtrl.verticalOrientation(value.x, value.y, value.z);
			final double ax = OrientationCtrl.horizontalOrientation(remoteState.orientation, value.x, value.y, value.z);

			handler.post(new Runnable() {public void run()
			{
				verticalIndicator.setRotation2((float) az - 90.0f);
				horizontalIndicator.setRotation2((float) ax);
			}});*/
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
		public void onStatus(Status status)
		{
			if (status == Status.RESUMED && resumed)
			{
				handshake();
			}
		}
	};

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();

		setStatus(Status.READY);
		startPreview();
	}

	private boolean resumed = false;

	private MasterShake masterShake;

	private void handshake()
	{
		if (masterShake == null)
			masterShake = new MasterShake(this, cameraPair);

		masterShake.start(new MasterShake.Listener()
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
		Log.d("stereoCamera", "slave ready");

		masterShake = new MasterShake(this, cameraPair);
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
		handler.post(() -> {
			Log.d("stereoCamera", "slave setup failed");
			Toast.makeText(MainActivity.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
			pauseConnection();
			MainActivity.getInstance().popSubViews();
		});
	}
}

package com.munger.stereocamera.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.Preferences;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Ping;
import com.munger.stereocamera.bluetooth.command.master.commands.SetFacing;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;
import com.munger.stereocamera.bluetooth.command.master.commands.SetZoom;
import com.munger.stereocamera.bluetooth.utility.CalculateSync;
import com.munger.stereocamera.bluetooth.utility.FireShutter;
import com.munger.stereocamera.bluetooth.utility.PhotoFiles;
import com.munger.stereocamera.bluetooth.utility.RemoteState;
import com.munger.stereocamera.widget.OrientationWidget;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class MasterFragment extends PreviewFragment
{
	private ImageButton clickButton;
	private ImageView thumbnailView;
	private OrientationWidget horizIndicator;

	private BluetoothMasterComm masterComm;
	private RemoteState remoteState;

	private long shutterDelay;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_master, container, false);
		super.onCreateView(rootView);

		clickButton = rootView.findViewById(R.id.shutter);
		thumbnailView = rootView.findViewById(R.id.thumbnail);
		horizIndicator = rootView.findViewById(R.id.horiz_level);

		thumbnailView.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			openThumbnail();
		}});

		shutterDelay = (long) MyApplication.getInstance().getPrefs().getShutterDelay();
		clickButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				new FireShutter(MasterFragment.this).execute(shutterDelay);
			}
		});

		return rootView;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	private MenuItem flipItem;
	private MenuItem syncItem;
	private MenuItem swapItem;


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.master_menu, menu);

		flipItem = menu.findItem(R.id.flip);
		syncItem = menu.findItem(R.id.sync);
		swapItem = menu.findItem(R.id.swap);

		updateHandPhoneButton();

		flipItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			boolean isFacing = MyApplication.getInstance().getPrefs().getIsFacing();
			isFacing = !isFacing;
			setFacing(isFacing);
			MyApplication.getInstance().getPrefs().setIsFacing(isFacing);

			masterComm.runCommand(new SetFacing(isFacing, new SetFacing.Listener()
			{
				@Override
				public void done()
				{}

				@Override
				public void fail()
				{}
			}));

			return true;
		}});

		syncItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem menuItem)
		{
			doSync();

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

	private void doSync()
	{
		CalculateSync sync = new CalculateSync(this, new CalculateSync.Listener()
		{
			@Override
			public void result(long localDelay)
			{
				MyApplication.getInstance().getPrefs().setShutterDelay(localDelay);
			}

			@Override
			public void fail()
			{}
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
		public void onStatus(PreviewFragment.Status status)
		{}

		@Override
		public void onFov(float horiz, float vert)
		{}

		@Override
		public void onZoom(float zoom)
		{
			MyApplication.getInstance().getPrefs().setRemoteZoom(zoom);
		}

		@Override
		public void onGravity(ReceiveGravity.Gravity value)
		{
			float zval = value.x * value.x + value.y * value.y;
			double azrad = Math.atan2(Math.sqrt(zval), value.z);
			final double az = azrad * 180.0 / Math.PI;

			handler.post(new Runnable() {public void run()
			{
				horizIndicator.setRotation2((float) az - 90.0f);
			}});
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

		updateThumbnail();
		onPreviewStarted1();
	}

	@Override
	protected void onZoom(int index)
	{
		MyApplication.getInstance().getPrefs().setLocalZoom(index);
	}

	private void updateThumbnail()
	{
		final PhotoFiles photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				new Handler(Looper.getMainLooper()).post(new Runnable() { public void run()
				{
					String max = photoFiles.getNewestFile().getPath();

					if (max == null)
					{
						thumbnailView.setImageDrawable(null);
						return;
					}
					else
					{
						Bitmap bmp = BitmapFactory.decodeFile(photoFiles.getFilePath(max));
						thumbnailView.setImageBitmap(bmp);
					}
				}});
			}
		});
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
				Log.d(getTag(), "slave ping failed");
				Toast.makeText(MyApplication.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
				((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
			}
		}));
	}

	private void onPreviewStarted2()
	{
		remoteState.waitOnReadyAsync(3000, new RemoteState.ReadyListener()
		{
			@Override
			public void done()
			{
				onPreviewStarted3();
			}

			@Override
			public void fail()
			{
				handler.post(new Runnable() {public void run()
				{
					Log.d(getTag(), "slave ready failed");
					Toast.makeText(MyApplication.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
					((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
				}});
			}
		});
	}

	private void onPreviewStarted3()
	{
		boolean isFacing = MyApplication.getInstance().getPrefs().getIsFacing();
		setFacing(isFacing);

		masterComm.runCommand(new SetFacing(isFacing, new SetFacing.Listener()
		{
			@Override
			public void done()
			{
				onPreviewStarted4();
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave set zoom failed");
			}
		}));
	}

	private void onPreviewStarted4()
	{

		float zoom = MyApplication.getInstance().getPrefs().getRemoteZoom();
		masterComm.runCommand(new SetZoom(zoom, new SetZoom.Listener()
		{
			@Override
			public void done()
			{
				Log.d(getTag(), "setup finish");
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave set facing failed");
			}
		}));
	}
}

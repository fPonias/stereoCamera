package com.munger.stereocamera;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.munger.stereocamera.fragment.Gallery;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.commands.Disconnect;
import com.munger.stereocamera.ip.command.commands.SendPhoto;
import com.munger.stereocamera.fragment.ConnectFragment;
import com.munger.stereocamera.fragment.HelpFragment;
import com.munger.stereocamera.fragment.ImageViewerFragment;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.SettingsFragment;
import com.munger.stereocamera.fragment.SlaveFragment;
import com.munger.stereocamera.ip.ethernet.EthernetCtrl;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.service.PhotoProcessorServiceReceiver;
import com.munger.stereocamera.utility.InteractiveReceiver;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.Preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity
{
	private static MainActivity instance;
	public static MainActivity getInstance()
	{
		return instance;
	}

	private Handler handler;
	private FrameLayout frame;
	private ConnectFragment connectFragment;
	private PhotoProcessorServiceReceiver photoReceiver;
	private InteractiveReceiver interactiveReceiver;

	public static final String BT_SERVICE_NAME = "stereoCamera";

	private CommCtrl ctrl;
	private BluetoothCtrl btCtrl;
	private EthernetCtrl ethCtrl;
	private Preferences prefs;

	public void setupBTServer(IPListeners.SetupListener listener)
	{
		if (btCtrl == null)
		{
			btCtrl = new BluetoothCtrl(this);
			MyApplication.getInstance().setBtCtrl(btCtrl);
		}

		if (!btCtrl.getIsSetup())
		{
			btCtrl.setup(listener);
			return;
		}

		listener.onSetup();
	}

	public void setCtrl(CommCtrl ctrl)
	{
		this.ctrl = ctrl;
	}

	public CommCtrl getCtrl()
	{
		return ctrl;
	}

	public BluetoothCtrl getBtCtrl()
	{
		return btCtrl;
	}

	public EthernetCtrl getEthCtrl()
	{
		return ethCtrl;
	}

	public void setupEthernetServer(IPListeners.SetupListener listener)
	{
		if (ethCtrl == null)
		{
			ethCtrl = new EthernetCtrl();
			MyApplication.getInstance().setEthCtrl(ethCtrl);
		}

		if (!ethCtrl.getIsSetup())
		{
			ethCtrl.setup(listener);
			return;
		}

		listener.onSetup();
	}

	public Preferences getPrefs()
	{
		return prefs;
	}

	public boolean getAdsEnabled() {return MyApplication.getInstance().getAdsEnabled();}

	public boolean getIsDebug() { return false; }//return BuildConfig.DEBUG; }

	public Fragment getCurrentFragment()
	{
		FragmentManager mgr = getSupportFragmentManager();
		List<Fragment> fragments = mgr.getFragments();

		if (fragments.size() == 0)
			return null;

		return fragments.get(0);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		MainActivity.instance = this;

		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);

		prefs = new Preferences();
		MyApplication.getInstance().setPrefs(prefs);
		prefs.setup();

		View root = getLayoutInflater().inflate(R.layout.activity_main, null);
		frame = root.findViewById(R.id.main_content);
		setContentView(root);

		if (savedInstanceState == null)
		{
			connectFragment = new ConnectFragment();
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.add(android.R.id.content, connectFragment, connectFragment.getTag());
			ft.disallowAddToBackStack();
			ft.commit();
		}
		else
		{
			if (savedInstanceState.containsKey("android:support:fragments"))
			{
				FragmentManager mgr = getSupportFragmentManager();
				List<Fragment> fragments = mgr.getFragments();
				int cnt = mgr.getBackStackEntryCount();

				for(Fragment fragment : fragments)
				{
					if (fragment instanceof ConnectFragment)
						connectFragment = (ConnectFragment) fragment;
					else if (fragment instanceof MasterFragment)
						masterFragment = (MasterFragment) fragment;
					else if (fragment instanceof SlaveFragment)
						slaveFragment = (SlaveFragment) fragment;
					else if (fragment instanceof HelpFragment)
						helpFragment = (HelpFragment) fragment;
					else if (fragment instanceof ImageViewerFragment)
						imageViewerFragment = (ImageViewerFragment) fragment;
					else if (fragment instanceof Gallery)
						galleryFragment = (Gallery) fragment;
					else if (fragment instanceof SettingsFragment)
						settingsFragment = (SettingsFragment) fragment;
				}
			}
		}

		handler = new Handler(Looper.getMainLooper());

		photoReceiver = new PhotoProcessorServiceReceiver(new PhotoProcessorServiceReceiver.Listener()
		{
			@Override
			public void onPhoto(String path)
			{
				handleProcessedPhoto(path);
			}
		});

		interactiveReceiver = new InteractiveReceiver();
		interactiveReceiver.addListener(new InteractiveReceiver.Listener()
		{
			@Override
			public void screenChanged(boolean isOn)
			{
				synchronized (listenerLock)
				{
					for (Listener listener : listeners)
						listener.onScreenChanged(isOn);
				}
			}
		});
	}

	private boolean isRunning = false;

	@Override
	protected void onPause()
	{
		super.onPause();

		unregisterReceiver(photoReceiver);
		unregisterReceiver(interactiveReceiver);

		isRunning = false;
	}

	@Override
	protected void onStart()
	{
		btCtrl = MyApplication.getInstance().getBtCtrl();
		ethCtrl = MyApplication.getInstance().getEthCtrl();

		FragmentManager ft = getSupportFragmentManager();
		backStackListener = new BackStackListener(ft);
		ft.addOnBackStackChangedListener(backStackListener);

		super.onStart();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		isRunning = true;
		btCtrl = MyApplication.getInstance().getBtCtrl();
		ethCtrl = MyApplication.getInstance().getEthCtrl();
		prefs = MyApplication.getInstance().getPrefs();

		IntentFilter filter = new IntentFilter(PhotoProcessorService.BROADCAST_PROCESSED_ACTION);
		registerReceiver(photoReceiver, filter, "com.munger.stereocamera.NOTIFICATION", new Handler(Looper.getMainLooper()));

		filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(interactiveReceiver, filter);

		filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		registerReceiver(interactiveReceiver, filter);

		reconnect();
	}

	public void reconnect()
	{
		Preferences.Roles role = prefs.getRole();
		if (ctrl != null && ctrl.getSocketCtrlCtrl().getIsSetup())
		{
			if (role == Preferences.Roles.MASTER)
			{
				SocketCtrl master = ctrl.getSocketCtrlCtrl().getSlave();
				if (master != null && master.isConnected())
				{
					startMasterView();
					return;
				}
			}
			else
			{
				SocketCtrl slave = ctrl.getSocketCtrlCtrl().getMaster();
				if (slave != null && slave.isConnected())
				{
					startSlaveView();
					return;
				}
			}
		}

		if (connectFragment != null && getCurrentFragment() == connectFragment)
		{
			if (prefs.getFirstTime() == true)
				return;

			connectFragment.reconnect();
		}
	}

	private class BackStackListener implements FragmentManager.OnBackStackChangedListener
	{
		public FragmentManager fragmentManager;
		public int lastCount;

		public BackStackListener(FragmentManager fm)
		{
			fragmentManager = fm;
			lastCount = 0;

			fragmentManager.addOnBackStackChangedListener(this);
		}

		public void onBackStackChanged()
		{
			int newCount = fragmentManager.getBackStackEntryCount();

			if (newCount == 0 && lastCount > 0)
			{
				if (ctrl != null)
					ctrl.sendCommand(new Disconnect(), null);

				slaveFragment = null;
				masterFragment = null;
			}

			lastCount = newCount;

		}
	}

	public void openSettings()
	{
		settingsFragment = new SettingsFragment();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("preview");
		ft.replace(android.R.id.content, settingsFragment, settingsFragment.getTag());
		ft.commit();
	}

	public void openHelp()
	{
		helpFragment = new HelpFragment();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("preview");
		ft.replace(android.R.id.content, helpFragment, helpFragment.getTag());
		ft.commit();
	}

	private BackStackListener backStackListener;
	private MasterFragment masterFragment;
	private SlaveFragment slaveFragment;
	private Gallery galleryFragment;
	private ImageViewerFragment imageViewerFragment;
	private SettingsFragment settingsFragment;
	private HelpFragment helpFragment;

	public void startMasterView()
	{
		if (masterFragment != null && getCurrentFragment() == masterFragment)
			return;

		if (masterFragment == null)
			masterFragment = new MasterFragment();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("connect");
		ft.replace(android.R.id.content, masterFragment, masterFragment.getTag());
		ft.commit();
	}

	public void startSlaveView()
	{
		if (slaveFragment != null && getCurrentFragment() == slaveFragment)
			return;

		if (slaveFragment == null)
			slaveFragment = new SlaveFragment();

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("connect");
		ft.replace(android.R.id.content, slaveFragment, slaveFragment.getTag());
		ft.commit();
	}

	public void startThumbnailView(final String path)
	{
		final PhotoFiles photoFiles = new PhotoFiles(this);
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				if (!photoFiles.hasFiles())
					return;

				imageViewerFragment = new ImageViewerFragment();
				imageViewerFragment.setStartingPath(path);
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.addToBackStack("imgView");
				ft.replace(android.R.id.content, imageViewerFragment, imageViewerFragment.getTag());
				ft.commit();
			}

			@Override
			public void fail()
			{
				final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
						.setMessage(R.string.thumbnail_filesystem_error)
						.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							dialogInterface.dismiss();
						}})
						.create();
				dialog.show();
			}
		});
	}

	public void startGalleryView()
	{
		final PhotoFiles photoFiles = new PhotoFiles(this);
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				if (!photoFiles.hasFiles())
					return;

				galleryFragment = new Gallery();
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.addToBackStack("imgView");
				ft.replace(android.R.id.content, galleryFragment, galleryFragment.getTag());
				ft.commit();
			}

			@Override
			public void fail()
			{
				final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
						.setMessage(R.string.thumbnail_filesystem_error)
						.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							dialogInterface.dismiss();
						}})
						.create();
				dialog.show();
			}
		});
	}

	public void popSubViews()
	{
		if (!isRunning)
		{
			return;
		}

		if (getCurrentFragment() != connectFragment)
		{
			FragmentManager mgr = getSupportFragmentManager();
			int sz = mgr.getBackStackEntryCount();
			for (int i = 0; i < sz; i++)
				mgr.popBackStack();
		}
	}

	public void popView()
	{
		FragmentManager mgr = getSupportFragmentManager();
		mgr.popBackStack();

		List<Fragment> frags = mgr.getFragments();
	}

	private boolean firstConnect = true;

	@Override
	protected void onUserLeaveHint()
	{
		super.onUserLeaveHint();

		Fragment frag = getCurrentFragment();
		if (frag instanceof MasterFragment)
		{
			((MasterFragment) frag).pause();
		}
		else if (frag instanceof SlaveFragment)
		{
			((SlaveFragment) frag).pause();
		}
	}


	private PhotoFiles photoFiles = null;

	public void handleProcessedPhoto(final String path)
	{
		if (photoFiles == null)
			photoFiles = new PhotoFiles(this);

		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				handlePhotoProcessed2(path);
			}

			@Override
			public void fail()
			{

			}
		});
	}

	private void handlePhotoProcessed2(String path)
	{
		File fl = new File(path);
		String newPath = photoFiles.saveNewFile(fl);

		//Toast.makeText(MainActivity.this, R.string.new_photo_available, Toast.LENGTH_LONG).show();

		if (ctrl != null && ctrl.isMaster())
		{
			ctrl.sendCommand(new SendPhoto(newPath), null);
		}

		onNewPhoto(newPath);
	}


	public static class Listener
	{
		public void onScreenChanged(boolean isOn) {}
		public void onBluetoothChanged(boolean isConnected, BluetoothDevice device) {}
		public void onNewPhoto(String newPath) {}
	}

	private ArrayList<Listener> listeners = new ArrayList<>();
	private final Object listenerLock = new Object();

	public void addListener(Listener listener)
	{
		synchronized (listenerLock)
		{
			listeners.add(listener);
		}
	}

	public void removeListener(Listener listener)
	{
		synchronized (listenerLock)
		{
			listeners.remove(listener);
		}
	}

	public void onNewPhoto(String path)
	{
		for (Listener listener : listeners)
			listener.onNewPhoto(path);
	}

	public void handleDisconnection()
	{
		synchronized (listenerLock)
		{
			for (Listener listener : listeners)
				listener.onBluetoothChanged(false, null);
		}
	}

	public PhotoOrientation getCurrentOrientation()
	{
		int rotation = MainActivity.getInstance().getWindowManager().getDefaultDisplay().getRotation();

		switch (rotation)
		{
			case Surface.ROTATION_0: return PhotoOrientation.DEG_0;
			case Surface.ROTATION_90: return PhotoOrientation.DEG_90;
			case Surface.ROTATION_180: return PhotoOrientation.DEG_180;
			case Surface.ROTATION_270: return PhotoOrientation.DEG_270;
			default: return PhotoOrientation.DEG_0;
		}
	}
}

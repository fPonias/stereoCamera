package com.munger.stereocamera;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.room.Room;

import com.munger.stereocamera.fragment.ConnectFragment;
import com.munger.stereocamera.fragment.Gallery;
import com.munger.stereocamera.fragment.HelpFragment;
import com.munger.stereocamera.fragment.ImageViewerFragment;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.SettingsFragment;
import com.munger.stereocamera.fragment.SlaveFragment;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.ip.command.commands.Disconnect;
import com.munger.stereocamera.service.PhotoProcessorWorker;
import com.munger.stereocamera.utility.InteractiveReceiver;
import com.munger.stereocamera.utility.PhotoFile;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.utility.data.AppDatabase;
import com.munger.stereocamera.utility.data.ClientViewModel;
import com.munger.stereocamera.utility.data.ClientViewModelProvider;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
	private static MainActivity instance;
	public static MainActivity getInstance()
	{
		return instance;
	}

	private Handler handler;
	//private FrameLayout frame;
	private ConnectFragment connectFragment;
	private InteractiveReceiver interactiveReceiver;
	public PhotoProcessorWorker photoProcessorWorker;
	private ClientViewModel clientViewModel;

	public ClientViewModel getClientViewModel() {return clientViewModel;}

	public boolean getAdsEnabled() {return MyApplication.getInstance().getAdsEnabled();}

	public boolean getIsDebug() { return false; }//return BuildConfig.DEBUG; }

	public Fragment getCurrentFragment()
	{
		MyApplication.getInstance();
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

		//setT(R.style.AppTheme);
		super.onCreate(savedInstanceState);

		new Thread(() ->{
 			clientViewModel = new ClientViewModelProvider(this).get(ClientViewModel.class);

 			runOnUiThread(() ->
			{
				openInitialView(savedInstanceState);
			});
		}).start();

		//View root = getLayoutInflater().inflate(R.layout.activity_main, null);
		//FrameLayout frame = root.findViewById(R.id.main_content);
		//setContentView(root);

		handler = new Handler(Looper.getMainLooper());

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

		photoProcessorWorker = new PhotoProcessorWorker(this);

		reconnect();
	}

	private void openInitialView(Bundle savedInstanceState)
	{
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
	}

	private boolean isRunning = false;

	@Override
	protected void onPause()
	{
		super.onPause();

		//unregisterReceiver(photoReceiver);
		//unregisterReceiver(interactiveReceiver);

		isRunning = false;
	}

	protected ActivityResultLauncher<String> permLauncher;
	protected ActivityResultLauncher<Intent> btLauncher;

	@Override
	protected void onStart()
	{
		FragmentManager ft = getSupportFragmentManager();
		backStackListener = new BackStackListener(ft);
		ft.addOnBackStackChangedListener(backStackListener);

		super.onStart();

		permLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), this::onRequestPermissionsResult);
		btLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onEnableBluetoothResult);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		//MyApplication.getInstance().getPrefs().setup();

		isRunning = true;
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
				CommCtrl ctrl = MyApplication.getInstance().getCtrl();
				if (ctrl != null)
				{
					ctrl.sendCommand(new Disconnect(), null);
					ctrl.cleanUpConnections();
				}

				slaveFragment = null;
				masterFragment = null;
			}

			lastCount = newCount;

		}
	}

	public boolean isOnStack(Fragment fragment)
	{

		FragmentManager mgr = getSupportFragmentManager();
		mgr.popBackStack();

		List<Fragment> frags = mgr.getFragments();

		return false;
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

	public void startThumbnailView(final PhotoFile data)
	{
		final PhotoFiles photoFiles = PhotoFiles.Factory.get();
		if (photoFiles.isEmpty())
			return;

		imageViewerFragment = new ImageViewerFragment();
		imageViewerFragment.setStartingData(data);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("imgView");
		ft.replace(android.R.id.content, imageViewerFragment, imageViewerFragment.getTag());
		ft.commit();
	}

	public void startGalleryView()
	{
		final PhotoFiles photoFiles = PhotoFiles.Factory.get();
		if (photoFiles.isEmpty())
			return;

		galleryFragment = new Gallery();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("imgView");
		ft.replace(android.R.id.content, galleryFragment, galleryFragment.getTag());
		ft.commit();
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

	/*
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
	*/


	private PhotoFiles photoFiles = null;

	public static class Listener
	{
		public void onScreenChanged(boolean isOn) {}
		public void onBluetoothChanged(boolean isConnected, BluetoothDevice device) {}
		public void onNewPhoto(Uri newPath) {}
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

	public void onNewPhoto(Uri path)
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

	public void reconnect()
	{
		Preferences prefs = new Preferences();
		CommCtrl ctrl = MyApplication.getInstance().getCtrl();

		if (ctrl != null && ctrl.getSocketCtrlCtrl().getIsSetup())
		{
			/*if (role == Preferences.Roles.MASTER)
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
			}*/
		}

		if (connectFragment != null && getCurrentFragment() == connectFragment)
		{
			if (prefs.getFirstTime() == true)
				return;

			connectFragment.reconnect();
		}
	}

	public PhotoOrientation getCurrentOrientation()
	{
		int rotation = getDisplay().getRotation();

		switch (rotation)
		{
			case Surface.ROTATION_0: return PhotoOrientation.DEG_0;
			case Surface.ROTATION_90: return PhotoOrientation.DEG_90;
			case Surface.ROTATION_180: return PhotoOrientation.DEG_180;
			case Surface.ROTATION_270: return PhotoOrientation.DEG_270;
			default: return PhotoOrientation.DEG_0;
		}
	}

	public interface PermissionResultListener
	{
		void onResult(boolean result);
	}

	private PermissionResultListener resultListener;
	private String requestedPermissions;

	public boolean hasPermission(String permission)
	{
		if (ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
			return false;

		return true;
	}

	public void requestPermissionForResult(String permissions, PermissionResultListener listener)
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			listener.onResult(false);
		else
			requestPermissionForResult2(permissions, listener);
	}

	private void requestPermissionForResult2(String permissions, PermissionResultListener listener)
	{
		resultListener = listener;
		requestedPermissions = permissions;

		permLauncher.launch(permissions);
	}

	protected void onRequestPermissionsResult(Boolean grantResults)
	{
		if (resultListener != null)
			resultListener.onResult(grantResults);
	}

	private BluetoothActivateListener btResultListener;

	public interface BluetoothActivateListener
	{
		void onResult(ActivityResult result);
	}

	public void enableBluetoothForResult(BluetoothActivateListener listener)
	{
		btResultListener = listener;
		btLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
	}

	public void bluetoothDiscoverForResult(BluetoothActivateListener listener)
	{
		btResultListener = listener;
		btLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
	}

	private void onEnableBluetoothResult(ActivityResult result)
	{
		btResultListener.onResult(result);
	}
}

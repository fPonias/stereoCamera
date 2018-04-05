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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.BluetoothSlave;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Disconnect;
import com.munger.stereocamera.bluetooth.command.master.commands.SendProcessedPhoto;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendDisconnect;
import com.munger.stereocamera.fragment.ConnectFragment;
import com.munger.stereocamera.fragment.HelpFragment;
import com.munger.stereocamera.fragment.ImageViewerFragment;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.SettingsFragment;
import com.munger.stereocamera.fragment.SlaveFragment;
import com.munger.stereocamera.service.PhotoProcessorService;
import com.munger.stereocamera.service.PhotoProcessorServiceReceiver;
import com.munger.stereocamera.utility.InteractiveReceiver;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.widget.OrientationCtrl;

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

	private BluetoothCtrl btCtrl;
	private OrientationCtrl orientationCtrl;
	private Preferences prefs;

	public void setupBTServer(BluetoothCtrl.SetupListener listener)
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

	public BluetoothCtrl getBtCtrl()
	{
		return btCtrl;
	}

	public Preferences getPrefs()
	{
		return prefs;
	}

	public OrientationCtrl getOrientationCtrl()
	{
		return orientationCtrl;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		MainActivity.instance = this;

		setTheme(R.style.AppTheme);
		super.onCreate(savedInstanceState);

		prefs = new Preferences();
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

			currentFragment = connectFragment;
		}

		setContentView(frame);

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
				for (Listener listener : listeners)
					listener.onScreenChanged(isOn);
			}
		});
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		unregisterReceiver(photoReceiver);
		unregisterReceiver(interactiveReceiver);
	}

	@Override
	protected void onStart()
	{
		btCtrl = MyApplication.getInstance().getBtCtrl();

		FragmentManager ft = getSupportFragmentManager();
		backStackListener = new BackStackListener(ft);
		ft.addOnBackStackChangedListener(backStackListener);

		super.onStart();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		btCtrl = MyApplication.getInstance().getBtCtrl();

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
		if (btCtrl != null && btCtrl.getIsSetup())
		{
			if (role == Preferences.Roles.MASTER)
			{
				BluetoothMaster btMaster = btCtrl.getMaster();
				if (btMaster != null && btMaster.isConnected())
				{
					startMasterView();
					return;
				}
			}
			else
			{
				BluetoothSlave btSlave = btCtrl.getSlave();
				if (btSlave != null && btSlave.isConnected())
				{
					startSlaveView();
					return;
				}
			}
		}

		if (connectFragment != null)
		{
			if (prefs.getFirstTime() == true)
				return;

			if (role == Preferences.Roles.MASTER)
			{
				connectFragment.connect();
			}
			else if (role == Preferences.Roles.SLAVE)
			{
				connectFragment.listen();
			}
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
				if (masterFragment != null)
					sendMasterDisconnect();
				if (slaveFragment != null)
					sendSlaveDisconnect();

				slaveFragment = null;
				masterFragment = null;
				currentFragment = connectFragment;
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
	private Fragment currentFragment;
	private MasterFragment masterFragment;
	private SlaveFragment slaveFragment;
	private ImageViewerFragment imgViewFragment;
	private SettingsFragment settingsFragment;
	private HelpFragment helpFragment;

	public void startMasterView()
	{
		if (masterFragment != null && currentFragment == masterFragment)
			return;

		masterFragment = new MasterFragment();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("connect");
		ft.replace(android.R.id.content, masterFragment, masterFragment.getTag());
		ft.commit();

		currentFragment = masterFragment;
	}

	public void startSlaveView()
	{
		if (slaveFragment != null && currentFragment == slaveFragment)
			return;

		slaveFragment = new SlaveFragment();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("connect");
		ft.replace(android.R.id.content, slaveFragment, slaveFragment.getTag());
		ft.commit();

		currentFragment = slaveFragment;
	}

	public void startThumbnailView()
	{
		final PhotoFiles photoFiles = new PhotoFiles(this);
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				if (!photoFiles.hasFiles())
					return;

				imgViewFragment = new ImageViewerFragment();
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.addToBackStack("imgView");
				ft.replace(android.R.id.content, imgViewFragment, imgViewFragment.getTag());
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
		if (currentFragment != connectFragment)
		{
			FragmentManager mgr = getSupportFragmentManager();
			int sz = mgr.getBackStackEntryCount();
			for (int i = 0; i < sz; i++)
				mgr.popBackStack();

			currentFragment = connectFragment;
		}
	}

	public void popView()
	{
		FragmentManager mgr = getSupportFragmentManager();
		mgr.popBackStack();

		List<Fragment> frags = mgr.getFragments();
	}

	private void sendMasterDisconnect()
	{
		if (btCtrl == null)
			return;

		BluetoothMaster master = btCtrl.getMaster();
		if (master == null)
			return;

		BluetoothMasterComm comm = master.getComm();
		if (comm == null)
			return;

		comm.runCommand(new Disconnect(), null);
	}

	private void sendSlaveDisconnect()
	{
		if (btCtrl == null)
			return;

		BluetoothSlave slave = btCtrl.getSlave();
		if (slave == null)
			return;

		BluetoothSlaveComm comm = slave.getComm();
		if (comm == null)
			return;

		comm.sendCommand(new SendDisconnect());
	}

	private boolean firstConnect = true;

	@Override
	protected void onUserLeaveHint()
	{
		super.onUserLeaveHint();

		if (currentFragment instanceof MasterFragment)
		{
			((MasterFragment) currentFragment).pause();
		}
	}


	private PhotoFiles photoFiles = null;

	private void handleProcessedPhoto(final String path)
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

		Toast.makeText(MainActivity.this, R.string.new_photo_available, Toast.LENGTH_LONG).show();

		if (btCtrl != null && btCtrl.getMaster() != null && btCtrl.getMaster().getComm() != null)
		{
			BluetoothMasterComm comm = btCtrl.getMaster().getComm();
			comm.runCommand(new SendProcessedPhoto(newPath), null);
		}

		for (Listener listener : listeners)
			listener.onNewPhoto(newPath);
	}


	public static class Listener
	{
		public void onScreenChanged(boolean isOn) {}
		public void onBluetoothChanged(boolean isConnected, BluetoothDevice device) {}
		public void onNewPhoto(String newPath) {}
	}

	private ArrayList<Listener> listeners = new ArrayList<>();

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	public void handleDisconnection()
	{
		for (Listener listener : listeners)
			listener.onBluetoothChanged(false, null);
	}


	public boolean isMasterConnected()
	{
		if (btCtrl == null)
			return false;

		BluetoothMaster master = btCtrl.getMaster();

		if (master == null)
			return false;

		return master.isConnected();
	}

	public boolean isSlaveConnected()
	{
		if (btCtrl == null)
			return false;

		BluetoothSlave slave = btCtrl.getSlave();

		if (slave == null)
			return false;

		return slave.isConnected();
	}

	public void cleanUpConnections()
	{
		if (btCtrl == null)
			return;

		BluetoothMaster btmaster = btCtrl.getMaster();
		if (btmaster != null && isMasterConnected())
			btmaster.cleanUp();

		BluetoothSlave btslave = btCtrl.getSlave();
		if (btslave != null && isSlaveConnected())
			btslave.cleanUp();
	}
}

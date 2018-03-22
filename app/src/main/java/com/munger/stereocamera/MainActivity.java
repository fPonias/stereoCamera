package com.munger.stereocamera;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.BluetoothSlave;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.commands.Disconnect;
import com.munger.stereocamera.bluetooth.command.slave.BluetoothSlaveComm;
import com.munger.stereocamera.bluetooth.command.slave.senders.SendDisconnect;
import com.munger.stereocamera.fragment.SettingsFragment;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.fragment.ConnectFragment;
import com.munger.stereocamera.fragment.ImageViewerFragment;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.SlaveFragment;

import java.util.List;

public class MainActivity extends BaseActivity
{
	private Handler handler;
	private FrameLayout frame;
	private ConnectFragment connectFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

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
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		FragmentManager ft =getSupportFragmentManager();
		backStackListener = new BackStackListener(ft);
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

				BluetoothCtrl ctrl = MyApplication.getInstance().getBtCtrl();

				if (ctrl != null)
					ctrl.cleanUp();
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

	private BackStackListener backStackListener;
	private Fragment currentFragment;
	private MasterFragment masterFragment;
	private SlaveFragment slaveFragment;
	private ImageViewerFragment imgViewFragment;
	private SettingsFragment settingsFragment;

	public void startMasterView()
	{
		masterFragment = new MasterFragment();
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack("connect");
		ft.replace(android.R.id.content, masterFragment, masterFragment.getTag());
		ft.commit();

		currentFragment = masterFragment;
	}

	public void startSlaveView()
	{
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
		BluetoothCtrl ctrl = MyApplication.getInstance().getBtCtrl();
		if (ctrl == null)
			return;

		BluetoothMaster master = ctrl.getMaster();
		if (master == null)
			return;

		BluetoothMasterComm comm = master.getComm();
		if (comm == null)
			return;

		comm.runCommand(new Disconnect());

		master.cleanUp();
	}

	private void sendSlaveDisconnect()
	{
		BluetoothCtrl ctrl = MyApplication.getInstance().getBtCtrl();
		if (ctrl == null)
			return;

		BluetoothSlave slave = ctrl.getSlave();
		if (slave == null)
			return;

		BluetoothSlaveComm comm = slave.getComm();
		if (comm == null)
			return;

		comm.sendCommand(new SendDisconnect());

		slave.cleanUp();
	}

	private boolean firstConnect = true;

	public void handleNewConnection(BluetoothDevice device)
	{
		Log.d("stereoCamera", "bluetooh device connected " + device.getName());
	}

	public void handleDisconnection(BluetoothDevice device)
	{
		Log.d("stereoCamera", "bluetooh device disconnected " + device.getName());

		popSubViews();
		firstConnect = true;
	}

	@Override
	protected void onUserLeaveHint()
	{
		super.onUserLeaveHint();
		popSubViews();
		firstConnect = true;
	}
}

package com.munger.stereocamera;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.munger.stereocamera.bluetooth.Preferences;
import com.munger.stereocamera.bluetooth.utility.PhotoFiles;
import com.munger.stereocamera.fragment.ConnectFragment;
import com.munger.stereocamera.fragment.ImageViewerFragment;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.SlaveFragment;

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
	}

	private Fragment currentFragment;
	private MasterFragment masterFragment;
	private SlaveFragment slaveFragment;
	private ImageViewerFragment imgViewFragment;

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
		final PhotoFiles photoFiles = new PhotoFiles();
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
	}

	private boolean firstConnect = true;

	public void handleNewConnection(BluetoothDevice device)
	{
		Log.d("stereoCamera", "bluetooh device connected " + device.getName());
	}

	public void handleDisconnection(BluetoothDevice device)
	{
		Log.d("stereoCamera", "bluetooh device disconnected " + device.getName());

		Preferences prefs = MyApplication.getInstance().getPrefs();

		if (prefs.getRole() == Preferences.Roles.SLAVE)
		{
			popSubViews();
			firstConnect = true;
		}
		else
		{
			popSubViews();
			firstConnect = true;
		}
	}
}

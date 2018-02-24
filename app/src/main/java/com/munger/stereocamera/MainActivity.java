package com.munger.stereocamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.fragment.ConnectFragment;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.fragment.MasterFragment2;
import com.munger.stereocamera.fragment.SlaveFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
{
	public static final String BT_SERVICE_NAME = "stereoCamera";

	public static int DISCOVER_TIMEOUT = 30000;
	public static int LISTEN_TIMEOUT = 300000;

	// Used to load the 'native-lib' library on application startup.
	static
	{
		System.loadLibrary("native-lib");
	}


	private BluetoothCtrl btCtrl = null;
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

		instance = this;
	}

	private static MainActivity instance;
	public static MainActivity getInstance()
	{
		return instance;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		if (btCtrl != null)
			btCtrl.cleanUp();
	}

	public boolean setupBTServer()
	{
		if (btCtrl == null)
			btCtrl = new BluetoothCtrl(this);

		if (!btCtrl.getIsSetup())
		{
			btCtrl.setup();
		}

		return true;
	}

	public BluetoothCtrl getBtCtrl()
	{
		return btCtrl;
	}


	/**
	 * A native method that is implemented by the 'native-lib' native library,
	 * which is packaged with this application.
	 */
	public native String stringFromJNI();

	public interface ResultListener
	{
		void onResult(int resultCode, Intent data);
	}

	private HashMap<Integer, ResultListener> resultListeners = new HashMap<>();

	public void startActivityForResult(Intent i, ResultListener listener)
	{
		int code = (int)(Math.random() * 0x8000);
		resultListeners.put(code, listener);
		startActivityForResult(i, code);
	}

	public void requestPermissionForResult(String[] permissions, ResultListener listener)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			int code = (int) (Math.random() * 0x8000);
			resultListeners.put(code, listener);

			requestPermissions(permissions, code);
		}
		else
		{
			listener.onResult(0, null);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		ResultListener listener = resultListeners.get(requestCode);

		listener.onResult(resultCode, data);
		resultListeners.remove(requestCode);
	}

	private Fragment currentFragment;
	private MasterFragment masterFragment;
	private SlaveFragment slaveFragment;

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
}

package com.munger.stereocamera.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.BluetoothMasterComm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class MasterFragment extends PreviewFragment
{
	private boolean cameraReady = false;
	private boolean previewReady = false;
	private boolean previewStarted = false;

	private Button clickButton;
	private Button flipButton;

	private BluetoothMasterComm masterComm;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_master, container, false);
		super.onCreateView(rootView);

		clickButton = rootView.findViewById(R.id.shutter);
		flipButton = rootView.findViewById(R.id.flip);

		clickButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				fireShutter();
			}
		});

		flipButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				doFlip();
			}
		});

		return rootView;
	}

	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;

	private byte[] localData;
	private byte[] remoteData;

	private void fireShutter()
	{
		super.fireShutter(new ImageListener()
		{
			@Override
			public void onImage(byte[] bytes)
			{
				localData = bytes;
				boolean doNext = false;

				synchronized (lock)
				{
					localShutterDone = true;

					if (remoteShutterDone)
						doNext = true;
				}

				if (doNext)
					fireShutter2();
			}
		});

		masterComm.shutter(new BluetoothMasterComm.ShutterListener()
		{
			@Override
			public void onData(byte[] data)
			{
				remoteData = data;
				boolean doNext = false;

				synchronized (lock)
				{
					remoteShutterDone = true;

					if (localShutterDone)
						doNext = true;
				}

				if (doNext)
					fireShutter2();
			}

			@Override
			public void fail()
			{
				remoteData = null;
				boolean doNext = false;

				synchronized (lock)
				{
					remoteShutterDone = true;

					if (localShutterDone)
						doNext = true;
				}

				if (doNext)
					fireShutter2();
			}
		});
	}

	@Override
	public void onStart()
	{
		super.onStart();

		masterComm = MainActivity.getInstance().getBtCtrl().getMaster().getComm();
		Log.d(getTag(), "pinging slave phone");
		masterComm.ping(new BluetoothMasterComm.PingListener()
		{
			@Override
			public void pong(long diff)
			{
				Log.d(getTag(), "pinged slave phone for " + diff + " ms");
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave ping failed");
			}
		});
	}

	private void fireShutter2()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MainActivity.getInstance().requestPermissionForResult(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					fireShutter2();
				}
			});
			return;
		}

		File targetDir = MainActivity.getInstance().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		String[] files = targetDir.list();
		int max = 0;

		for (String filename : files)
		{
			String[] parts = filename.split("\\.");
			int count = Integer.parseInt(parts[0]);

			if (count > max)
				max = count;
		}

		max++;

		byte[] merged = processData(localData, remoteData);
		String localName = max + ".jpg";
		saveFile(localName, targetDir, merged);
		localName = max + ".left.jpg";
		saveFile(localName, targetDir, localData);
		localName = max + ".right.jpg";
		saveFile(localName, targetDir, remoteData);
	}

	private byte[] processData(byte[] left, byte[] right)
	{
		Bitmap leftBmp = BitmapFactory.decodeByteArray(left, 0, left.length);
		Bitmap rightBmp = BitmapFactory.decodeByteArray(right, 0, right.length);

		int h1 = leftBmp.getHeight();
		int w1 = leftBmp.getWidth();
		int h2 = rightBmp.getHeight();
		int w2 = rightBmp.getWidth();
		float scale = (float) w1 / (float) w2;

		int w = (h1 < h2) ? h1 : h2;

		Matrix m1 = new Matrix();
		m1.postRotate(90);
		Bitmap leftRotated = Bitmap.createBitmap(leftBmp, 0, 0, h1, h1, m1, true);

		Matrix m2 = new Matrix();
		m2.postScale(scale, scale);
		m2.postRotate(90);
		Bitmap rightRotated = Bitmap.createBitmap(rightBmp, 0, 0, h2, h2, m2, true);

		Bitmap out = Bitmap.createBitmap(w * 2, w, leftBmp.getConfig());

		int[] buffer = new int[w * w];
		leftRotated.getPixels(buffer, 0, w, 0, 0, w, w);
		out.setPixels(buffer, 0, w, 0, 0, w, w);
		leftRotated.recycle();

		rightRotated.getPixels(buffer, 0, w, 0, 0, w, w);
		out.setPixels(buffer, 0, w, w, 0, w, w);
		rightRotated.recycle();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.compress(Bitmap.CompressFormat.JPEG, 85, baos);
		byte[] ret = baos.toByteArray();
		return ret;
	}

	private void saveFile(String name, File targetDir, byte[] data)
	{
		try
		{
			File f = new File(targetDir, name);
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(data);
			fos.close();
		}
		catch(IOException e){

		}
	}


}

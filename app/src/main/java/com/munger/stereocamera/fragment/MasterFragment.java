package com.munger.stereocamera.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.BluetoothMasterComm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Policy;
import java.text.ParseException;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class MasterFragment extends PreviewFragment
{
	private boolean cameraReady = false;
	private boolean previewReady = false;
	private boolean previewStarted = false;

	private ImageButton clickButton;
	private ImageButton flipButton;
	private ImageView thumbnailView;

	private BluetoothMasterComm masterComm;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_master, container, false);
		super.onCreateView(rootView);

		clickButton = rootView.findViewById(R.id.shutter);
		flipButton = rootView.findViewById(R.id.flip);
		thumbnailView = rootView.findViewById(R.id.thumbnail);

		thumbnailView.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			openThumbnail();
		}});

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
				masterComm.flip(new BluetoothMasterComm.FlipListener()
				{
					@Override
					public void done()
					{
					}

					@Override
					public void fail()
					{
					}
				});
			}
		});

		return rootView;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
	}

	@Override
	protected void onPreviewStarted()
	{
		super.onPreviewStarted();

		updateThumbnail();
		onPreviewStarted1();
	}

	private void updateThumbnail()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MyApplication.getInstance().getCurrentActivity().requestPermissionForResult(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					updateThumbnail();
				}
			});
			return;
		}

		new Handler(Looper.getMainLooper()).post(new Runnable() { public void run()
		{
			File targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			int max = getNewestFile(targetDir);

			if (max == 0)
			{
				thumbnailView.setImageDrawable(null);
				return;
			}
			else
			{
				Bitmap bmp = BitmapFactory.decodeFile(targetDir.getPath() + "/" + max + ".jpg");
				thumbnailView.setImageBitmap(bmp);
			}
		}});
	}

	private void onPreviewStarted1()
	{
		if (remoteLatency != -1 && localLatency != -1)
			return;

		Log.d(getTag(), "pinging slave phone");
		masterComm.ping(new BluetoothMasterComm.PingListener()
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
			}
		});
	}

	private void onPreviewStarted2()
	{
		masterComm.getStatus(new BluetoothMasterComm.StatusListener()
		{
			@Override
			public void done(int status)
			{
				if (status != PreviewFragment.READY)
				{
					try
					{
						Thread.sleep(250);
					}
					catch (InterruptedException e)
					{
					}
					onPreviewStarted2();
				}
				else
				{
					onPreviewStarted3();
				}
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave status failed");
			}
		});
	}

	private void onPreviewStarted3()
	{
		masterComm.getAngleOfView(new BluetoothMasterComm.AngleOfViewListener()
		{
			@Override
			public void done(float horiz, float vert)
			{
				if (vert > -1.0f && verticalAngle > -1.0f)
				{
					float zoom = calculateZoom(horizontalAngle, horiz);

					if (zoom > 1.0f)
					{
						setZoom(null, zoom);
						onPreviewStarted5();
					}
					else
					{
						zoom = 1.0f / zoom;
						onPreviewStarted4(zoom);
					}
				}
				else
					onPreviewStarted5();
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave viewAngle failed");
			}
		});
	}

	private void onPreviewStarted4(double zoom)
	{
		masterComm.setZoom((float) zoom, new BluetoothMasterComm.ZoomListener()
		{
			@Override
			public void done()
			{
				onPreviewStarted5();
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave set zoom failed");
			}
		});
	}

	private void onPreviewStarted5()
	{
		synchronized (lock)
		{
			if (shutterFiring)
				try{lock.wait();}catch(InterruptedException e){return;}

			shutterFiring = true;
		}

		masterComm.latencyCheck(new BluetoothMasterComm.LatencyCheckListener()
		{
			@Override
			public void trigger(final BluetoothMasterComm.LatencyCheckListenerListener listener)
			{
				getLatency(new LatencyListener()
				{
					@Override
					public void triggered()
					{
						listener.reply();
					}

					@Override
					public void done()
					{
						synchronized (lock)
						{
							shutterFiring = false;
							lock.notify();
						}
					}
				});
			}

			@Override
			public void pong(long localLatency, long remoteLatency)
			{
				MasterFragment.this.localLatency = localLatency;
				MasterFragment.this.remoteLatency = remoteLatency;
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "slave latency failed");
				synchronized (lock)
				{
					shutterFiring = false;
					lock.notify();
				}
			}
		});

		synchronized (lock)
		{
			if (shutterFiring)
			{
				try {lock.wait(2000); } catch(InterruptedException e){}
				shutterFiring = false;
			}
		}
	}

	private long localLatency = -1;
	private long remoteLatency = -1;

	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;

	private byte[] localData;
	private byte[] remoteData;

	private void fireShutter()
	{
		synchronized (lock)
		{
			if (shutterFiring)
				return;

			shutterFiring = true;
		}

		Thread t = new Thread(new Runnable() {public void run()
		{
			long diff = remoteLatency - localLatency;
			try { Thread.sleep(diff); } catch(InterruptedException e){}

			MasterFragment.super.fireShutter(new ImageListener()
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

					shutterFiring = false;
					lock.notify();
				}

				if (doNext)
					fireShutter2();
				}
			});
		}});
		t.start();

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

	private void fireShutter2()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MyApplication.getInstance().getCurrentActivity().requestPermissionForResult(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					fireShutter2();
				}
			});
			return;
		}

		File targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		int max = getNewestFile(targetDir);
		max++;

		byte[] merged = processData(localData, remoteData);
		String localName = max + ".jpg";
		saveFile(localName, targetDir, merged);

		//localName = max + ".left.jpg";
		//saveFile(localName, targetDir, localData);
		//localName = max + ".right.jpg";
		//saveFile(localName, targetDir, remoteData);

		updateThumbnail();
	}

	private int getNewestFile(File targetDir)
	{
		String[] files = targetDir.list();
		int max = 0;

		for (String filename : files)
		{
			String[] parts = filename.split("\\.");

			try
			{
				int count = Integer.parseInt(parts[0]);

				if (count > max)
					max = count;
			}
			catch(NumberFormatException e) {}
		}

		return max;
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

			MediaScannerConnection.scanFile(MyApplication.getInstance(), new String[]{f.getPath()}, null, null);
		}
		catch(IOException e){

		}
	}

	private byte[] loadFile(String name, File targetDir)
	{
		byte[] ret = null;
		try
		{
			File f = new File(targetDir, name);
			FileInputStream fis = new FileInputStream(f);

			int sz = (int) f.length();
			ret = new byte[sz];

			int read = 0;
			int total = 0;
			while (total < sz)
			{
				read = fis.read(ret, total, sz - total);
				total += read;
			}
		}
		catch(IOException e){

		}

		return ret;
	}

	private void openThumbnail()
	{
		BaseActivity act = MyApplication.getInstance().getCurrentActivity();
		if (act instanceof MainActivity)
			((MainActivity) act).startThumbnailView();
	}
}

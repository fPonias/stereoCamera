package com.munger.stereocamera.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;

import java.io.IOException;

/**
 * Created by hallmarklabs on 2/23/18.
 */

public class PreviewFragment extends Fragment
{
	private final Object lock = new Object();
	private boolean cameraReady = false;
	private boolean previewReady = false;
	private boolean previewStarted = false;

	protected View rootView;
	private SurfaceView previewView;
	private SurfaceHolder preview;
	private int cameraId;
	private Camera camera;

	protected void onCreateView(View rootView)
	{
		this.rootView = rootView;
		previewView = rootView.findViewById(R.id.preview);

		Display d = MainActivity.getInstance().getWindowManager().getDefaultDisplay();
		DisplayMetrics m = new DisplayMetrics();
		d.getMetrics(m);

		int w = m.widthPixels;
		int h = m.heightPixels;

		if (h > w)
			previewView.setLayoutParams(new RelativeLayout.LayoutParams(w, w));
		else
			previewView.setLayoutParams(new RelativeLayout.LayoutParams(h, h));
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState)
	{
		preview = previewView.getHolder();

		preview.addCallback(new SurfaceHolder.Callback()
		{
			@Override
			public void surfaceCreated(SurfaceHolder surfaceHolder)
			{
				boolean doLoad = false;
				synchronized (lock)
				{
					previewReady = true;

					if (cameraReady)
						doLoad = true;
				}

				if (doLoad)
					startPreview();
			}

			@Override
			public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2)
			{

			}

			@Override
			public void surfaceDestroyed(SurfaceHolder surfaceHolder)
			{

			}
		});
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		camera.release();
	}

	@Override
	public void onStart()
	{
		super.onStart();

		cameraId = 0;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
		{
			MainActivity.getInstance().requestPermissionForResult(new String[]{Manifest.permission.CAMERA}, new MainActivity.ResultListener()
			{
				@Override
				public void onResult(int resultCode, Intent data)
				{
					startCamera();
				}
			});
			return;
		}

		startCamera();
	}

	private void startCamera()
	{
		camera = Camera.open(cameraId);
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);


		boolean doLoad = false;
		synchronized (lock)
		{
			cameraReady = true;

			if (previewReady)
				doLoad = true;
		}

		if (doLoad)
			startPreview();
	}

	private void startPreview()
	{
		synchronized (lock)
		{
			if (previewStarted)
				return;

			previewStarted = true;
		}

		try
		{
			setCameraDisplayOrientation(cameraId, camera);
			camera.setPreviewDisplay(preview);
		}
		catch(IOException e){
			camera.release();
			return;
		}

		camera.startPreview();
	}

	public static void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera)
	{
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = MainActivity.getInstance().getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;

		switch (rotation)
		{
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		{
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		}
		else
		{  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		camera.setDisplayOrientation(result);
	}

	private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
	{
		@Override
		public void onShutter()
		{
			shutterEnd = System.currentTimeMillis();
		}
	};

	private Camera.PictureCallback jpegCallback = new Camera.PictureCallback()
	{
		@Override
		public void onPictureTaken(byte[] bytes, Camera camera)
		{
			if (listener != null)
				listener.onImage(bytes);

			listener = null;
			camera.startPreview();
		}
	};

	public interface ImageListener
	{
		void onImage(byte[] bytes);
	}

	protected ImageListener listener;
	protected long shutterStart;
	protected long shutterEnd;

	public void fireShutter(ImageListener listener)
	{
		this.listener = listener;
		shutterStart = System.currentTimeMillis();
		camera.takePicture(shutterCallback, null, null, jpegCallback);
	}

	public void doFlip()
	{
		synchronized (lock)
		{
			previewStarted = false;
		}

		camera.release();
		cameraId = cameraId == 0 ? 1 : 0;

		startCamera();
	}
}

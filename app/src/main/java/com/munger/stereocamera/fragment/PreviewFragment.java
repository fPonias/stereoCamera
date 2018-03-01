package com.munger.stereocamera.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.hardware.Camera.Size;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;

import java.io.IOException;
import java.security.Policy;
import java.util.List;

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
	private TextureView previewView;
	private SurfaceTexture preview;
	private int cameraId;
	private Camera camera;

	public int status;

	public static final int CREATED = 1;
	public static final int READY = 2;
	public static final int BUSY = 3;

	public PreviewFragment()
	{
		status = CREATED;
	}

	protected void onCreateView(View rootView)
	{
		this.rootView = rootView;
		previewView = rootView.findViewById(R.id.preview);

		Display d = MyApplication.getInstance().getCurrentActivity().getWindowManager().getDefaultDisplay();
		DisplayMetrics m = new DisplayMetrics();
		d.getMetrics(m);

		int w = m.widthPixels;

		previewView.setLayoutParams(new RelativeLayout.LayoutParams(w, w));
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState)
	{
		preview = previewView.getSurfaceTexture();
		previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
		{
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1)
			{
				preview = surfaceTexture;
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
			public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1)
			{

			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
			{
				preview = null;
				camera.stopPreview();
				camera.release();

				synchronized (lock)
				{
					previewReady = false;
					previewStarted = false;
				}

				return true;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
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
		int sz = Camera.getNumberOfCameras();
		Camera.CameraInfo info = new Camera.CameraInfo();
		for (int i = 0; i < sz; i++)
		{
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			{
				cameraId = i;
				break;
			}
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
		{
			MyApplication.getInstance().getCurrentActivity().requestPermissionForResult(new String[]{Manifest.permission.CAMERA}, new MainActivity.ResultListener()
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

	protected float horizontalAngle = -1.0f;
	protected float verticalAngle = -1.0f;

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
			camera.setPreviewTexture(preview);

			Camera.Parameters parameters = camera.getParameters();
			horizontalAngle = parameters.getHorizontalViewAngle();
			verticalAngle = parameters.getVerticalViewAngle();
			setSizes(parameters);
			adjustCrop(parameters);
			camera.setParameters(parameters);
		}
		catch(IOException e){
			camera.release();
			return;
		}

		camera.startPreview();
		onPreviewStarted();
	}

	protected void setSizes(Camera.Parameters parameters)
	{
		float targetRatio = 640.0f / 480.0f;

		List<Size> sizes = parameters.getSupportedPreviewSizes();
		Size previewSize = getClosestSize(targetRatio, sizes);
		parameters.setPreviewSize(previewSize.width, previewSize.height);

		sizes = parameters.getSupportedPictureSizes();
		Size pictureSize = getClosestSize(targetRatio, sizes);
		parameters.setPictureSize(pictureSize.width, pictureSize.height);
	}

	private Size getClosestSize(float targetRatio, List<Size> sizes)
	{
		Size closestSize = null;
		float closestDiff = 0.0f;

		for (Size sz : sizes)
		{
			float ratio = (float) sz.width / (float) sz.height;
			float diff = Math.abs(targetRatio - ratio);

			if (closestSize == null || diff < closestDiff)
			{
				closestSize = sz;
				closestDiff = diff;
			}
			else if (Math.abs(diff - closestDiff) < 0.05f && sz.width > closestSize.width)
			{
				closestSize = sz;
				closestDiff = diff;
			}
		}

		return closestSize;
	}

	protected float calculateZoom(float localAngle, float remoteAngle)
	{
		float ret = remoteAngle / localAngle;

		return ret;
	}

	protected void setZoom(Camera.Parameters parameters, float zoom)
	{
		if (parameters == null)
			parameters = camera.getParameters();

		zoom *= 100.0f;

		int max = parameters.getMaxZoom();
		List<Integer> zoomList = parameters.getZoomRatios();

		double lastDiff = 0;
		int idx = -1;
		int z = zoomList.size();

		for (int i = 0; i < z; i++)
		{
			int zv = zoomList.get(i);
			double diff = Math.abs(zoom - zv);

			if (i == 0 || diff < lastDiff)
			{
				lastDiff = diff;
				idx = i;
			}
		}

		parameters.setZoom(idx);
		camera.setParameters(parameters);
	}

	protected void zoom(boolean in)
	{
		Camera.Parameters parameters = camera.getParameters();
		int max = parameters.getMaxZoom();
		int zoom = parameters.getZoom();
		List<Integer> zoomList = parameters.getZoomRatios();

		if (in)
			zoom = Math.min(zoom + 1, max);
		else
			zoom = Math.max(zoom - 1, 0);

		Log.d("PreviewFragment", "zoom set to index " + zoom + " zoom value " + zoomList.get(zoom));
		parameters.setZoom(zoom);
		camera.setParameters(parameters);
	}

	private void adjustCrop(Camera.Parameters parameters)
	{
		Camera.Size sz = parameters.getPreviewSize();
		int preWidth = previewView.getMeasuredWidth();

		float scaleX = (float) sz.width / (float) preWidth;

		if (scaleX < 1)
			scaleX = 1.0f / scaleX;

		Matrix m = new Matrix();
		m.setScale(1, scaleX);
		previewView.setTransform(m);
	}

	protected void onPreviewStarted()
	{
		status = READY;
	}

	public static void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera)
	{
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = MyApplication.getInstance().getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();
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
			status = READY;
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
		status = BUSY;
		this.listener = listener;
		shutterStart = System.currentTimeMillis();
		camera.takePicture(shutterCallback, null, null, jpegCallback);
	}

	public interface LatencyListener
	{
		void triggered();
		void done();
	}

	public void getLatency(final LatencyListener listener)
	{
		camera.takePicture(
			new Camera.ShutterCallback()
			{
				@Override
				public void onShutter()
				{
					listener.triggered();
				}
			},
			null, null,
			new Camera.PictureCallback()
			{
				@Override
				public void onPictureTaken(byte[] bytes, Camera camera)
				{
					camera.startPreview();
					listener.done();
				}
			}
		);
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

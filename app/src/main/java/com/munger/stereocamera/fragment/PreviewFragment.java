package com.munger.stereocamera.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.hardware.Camera.Size;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
import com.munger.stereocamera.bluetooth.command.PhotoOrientations;

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
	protected int cameraId;
	protected Camera camera;

	protected ScaleGestureDetector scaleDetector;

	private Status status;

	protected void setStatus(Status status)
	{
		this.status = status;
	}

	public enum Status
	{
		NONE,
		CREATED,
		READY,
		BUSY
	};

	public PreviewFragment()
	{
		setStatus(Status.CREATED);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		currentZoom = MyApplication.getInstance().getPrefs().getLocalZoom();
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

	@SuppressLint("ClickableViewAccessibility")
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

		scaleDetector = new ScaleGestureDetector(MyApplication.getInstance(), new ScaleDetector());

		previewView.setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent)
			{
				return scaleDetector.onTouchEvent(motionEvent);
			}
		});
	}

	protected void onZoom(int index)
	{}

	class ScaleDetector implements  ScaleGestureDetector.OnScaleGestureListener
	{
		private float zoom;
		private float startZoom;
		private int idx;
		private int idxMax;
		private List<Integer> zoomList;
		private Camera.Parameters parameters;
		private long startTime;

		@Override
		public boolean onScale(ScaleGestureDetector scaleGestureDetector)
		{
			float factor = scaleGestureDetector.getScaleFactor();
			float delta = startZoom * (factor - 1.0f);
			float newZoom = zoom + delta;
			Log.d("zoom", "new zoom: " + newZoom + " factor: " + factor);
			int newIdx = idx;

			float possibleZoom = zoomList.get(newIdx);
			float oldDiff = Math.abs(newZoom - possibleZoom);
			float newDiff;
			int bestIdx = idx;

			if (factor < 1.0f)
			{
				do
				{
					newIdx = Math.max(newIdx - 1, 0);
					possibleZoom = zoomList.get(newIdx);
					newDiff = Math.abs(newZoom * 100.0f - possibleZoom);

					if (newDiff < oldDiff)
						bestIdx = newIdx;
					else
						break;

					oldDiff = newDiff;
				} while(newIdx > 0);
			}
			else if (factor > 1.0f)
			{
				do
				{
					newIdx = Math.min(newIdx + 1, idxMax);
					possibleZoom = zoomList.get(newIdx);
					newDiff = Math.abs(newZoom * 100.0f - possibleZoom);

					if (newDiff < oldDiff)
						bestIdx = newIdx;
					else
						break;

					oldDiff = newDiff;
				} while(newIdx < idxMax - 1);
			}

			if (bestIdx != idx)
			{
				Log.d("zoom", "new zoom index: " + bestIdx);
				parameters.setZoom(bestIdx);
				camera.setParameters(parameters);

				float myZoom = (float) zoomList.get(bestIdx) / 100.0f;
				onZoomUpdated(myZoom);
			}

			zoom = newZoom;

			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
		{
			parameters = camera.getParameters();
			zoomList = parameters.getZoomRatios();
			idx = parameters.getZoom();
			idxMax = parameters.getMaxZoom();
			zoom = (float) zoomList.get(idx) / 100.0f;
			startZoom = zoom;
			startTime = System.currentTimeMillis();

			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector scaleGestureDetector)
		{

		}
	}

	protected void onZoomUpdated(float zoom)
	{}

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
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt("currentZoom", currentZoom);
		outState.putInt("cameraId", cameraId);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState)
	{
		super.onViewStateRestored(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.containsKey("cameraId"))
		{
			cameraId = savedInstanceState.getInt("cameraId");
			currentZoom = savedInstanceState.getInt("currentZoom");
		}
		else
		{
			currentZoom = 0;
		}
	}

	public boolean isFrontFacing()
	{
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
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
	protected int minZoom = 0;
	protected int maxZoom = 0;

	private void startPreview()
	{
		new Handler(Looper.getMainLooper()).post(new Runnable() {public void run()
		{
			synchronized (lock)
			{
				if (previewStarted)
					return;

				previewStarted = true;
			}

			try
			{
				updatePreviewParameters();

				camera.setPreviewTexture(preview);
			}
			catch(IOException e){
				camera.release();
				return;
			}

			camera.startPreview();
			onPreviewStarted();
		}});
	}

	protected void updatePreviewParameters()
	{
		setCameraDisplayOrientation(cameraId, camera);

		Camera.Parameters parameters = camera.getParameters();
		horizontalAngle = parameters.getHorizontalViewAngle();
		verticalAngle = parameters.getVerticalViewAngle();
		maxZoom = parameters.getMaxZoom();
		setSizes(parameters);
		adjustCrop(parameters);
		parameters.setZoom(currentZoom);
		camera.setParameters(parameters);
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

	protected int currentZoom = 0;

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

		currentZoom = idx;
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
		currentZoom = zoom;
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

	public PhotoOrientations getCurrentOrientation()
	{
		int rotation = MyApplication.getInstance().getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();

		switch (rotation)
		{
			case Surface.ROTATION_0: return PhotoOrientations.DEG_0;
			case Surface.ROTATION_90: return PhotoOrientations.DEG_90;
			case Surface.ROTATION_180: return PhotoOrientations.DEG_180;
			case Surface.ROTATION_270: return PhotoOrientations.DEG_270;
			default: return PhotoOrientations.DEG_0;
		}
	}

	public float getZoomValue()
	{
		Camera.Parameters parameters = camera.getParameters();
		int zoom = parameters.getZoom();
		List<Integer> zoomList = parameters.getZoomRatios();

		float ret = zoomList.get(zoom);
		return ret / 100.0f;
	}

	protected void onPreviewStarted()
	{
		setStatus(Status.READY);
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
			setStatus(Status.READY);
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
		setStatus(Status.BUSY);
		this.listener = listener;
		shutterStart = System.currentTimeMillis();
		camera.takePicture(shutterCallback, null, null, jpegCallback);
	}

	public interface LatencyListener
	{
		void triggered();
		void done();
		void fail();
	}

	public void getLatency(final long delay, final LatencyListener listener)
	{
		setStatus(Status.BUSY);
		try{Thread.sleep(delay);} catch (InterruptedException e){}

		Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback()
		{
			@Override
			public void onShutter()
			{
				listener.triggered();
			}
		};

		Camera.PictureCallback pictureCallback = new Camera.PictureCallback()
		{
			@Override
			public void onPictureTaken(byte[] bytes, Camera camera)
			{
				for (int i = 0; i < 5; i++)
				{
					try
					{
						camera.startPreview();
						break;
					}
					catch(RuntimeException e){
						try{Thread.sleep(300);}catch(InterruptedException e1){}
					}
				}

				onPreviewStarted();
				listener.done();
			}
		};

		try
		{
			camera.takePicture(shutterCallback,null, null, pictureCallback);
		}
		catch(RuntimeException e){
			listener.fail();
		}
	}

	public void setFacing(boolean facing)
	{
		if (isFrontFacing() == facing)
			return;

		cameraId = 0;
		int targetFace = (facing) ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;


		int sz = Camera.getNumberOfCameras();
		Camera.CameraInfo info = new Camera.CameraInfo();
		for (int i = 0; i < sz; i++)
		{
			Camera.getCameraInfo(i, info);
			if (info.facing == targetFace)
			{
				cameraId = i;
				break;
			}
		}

		if (camera != null)
		{
			camera.stopPreview();
			camera.release();
		}

		synchronized (lock)
		{
			previewStarted = false;
		}

		startCamera();
	}
}

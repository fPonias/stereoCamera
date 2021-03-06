package com.munger.stereocamera.widget;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.command.PhotoOrientation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class PreviewWidget extends TextureView
{

	public PreviewWidget(Context context)
	{
		super(context);
	}

	public PreviewWidget(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public PreviewWidget(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	private int cameraId;
	private Camera camera;
	private float zoom = 1.0f;
	private PhotoOrientation orientation;
	private boolean facing;
	private Camera.Parameters currentParameters = null;

	protected ScaleGestureDetector scaleDetector;

	private SurfaceTexture preview;
	private boolean previewReady = false;
	private boolean previewStarted = false;
	private final Object lock = new Object();

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		Resources res = getContext().getResources();
		DisplayMetrics dm = res.getDisplayMetrics();
		int hret = dm.heightPixels;

		int id = res.getIdentifier("status_bar_height", "dimen", "android");
		if (id > 0)
		{
			int h = res.getDimensionPixelSize(id);
			hret -= h;
		}

		TypedValue tv = new TypedValue();
		if (getContext().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
		{
			int h = TypedValue.complexToDimensionPixelSize(tv.data, res.getDisplayMetrics());
			hret -= h;
		}


		int max = Math.min(dm.widthPixels, hret);
		setMeasuredDimension(max, max);
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		preview = getSurfaceTexture();
		setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
		{
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1)
			{
				preview = surfaceTexture;
				synchronized (lock)
				{
					previewReady = true;
				}

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
				synchronized (lock)
				{
					previewReady = false;
				}

				stopPreview();

				return true;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
			{

			}
		});

		scaleDetector = new ScaleGestureDetector(MainActivity.getInstance(), new ScaleDetector());

		setOnTouchListener(new View.OnTouchListener()
		{
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent)
			{
				return scaleDetector.onTouchEvent(motionEvent);
			}
		});
	}

	public void cleanUp()
	{
		releaseCamera();
	}

	private void setCamera(Camera camera)
	{
		synchronized (lock)
		{
			previewStarted = false;
			this.camera = camera;
		}

		currentParameters = this.camera.getParameters();

		Pair p = getCameraResolution(cameraId, camera);
		setupTexture(camera);

		String tag = "stereoCamera";
		Log.d("stereoCamera", "camera " + cameraId + " selected");
		Log.d("stereoCamera", "camera focal length " + currentParameters.getFocalLength() + "mm");
		Log.d("stereoCamera", "camera angle of view " + currentParameters.getHorizontalViewAngle() + ", " + currentParameters.getVerticalViewAngle());
		Log.d("stereoCamera", "camera picture size " + currentParameters.getPictureSize().width + "x" + currentParameters.getPictureSize().height);
		Log.d("stereoCamera", "camera sensor size " + p.w + "x" + p.h);

		updateTransform();
		startPreview();
	}

	private void setupTexture(Camera camera)
	{
		if (currentParameters == null)
			currentParameters = camera.getParameters();

		List<Camera.Size> presizes = currentParameters.getSupportedPreviewSizes();
		List<Camera.Size> picsizes = currentParameters.getSupportedPictureSizes();

		int sz = picsizes.size();
		Camera.Size picSize = null;
		for (int i = 0; i < sz; i++)
		{
			Camera.Size ps = picsizes.get(i);

			if (ps.height >= 480)
			{
				picSize = ps;
				break;
			}
		}

		if (picSize == null)
			picSize = picsizes.get(0);

		float ratio = (float) picSize.width / (float) picSize.height;

		float closestDiff = 1000000;
		int closestIdx = -1;
		int closestWidth = 0;
		sz = presizes.size();
		for (int i = 0; i < sz; i++)
		{
			Camera.Size preSize = presizes.get(i);
			float picRatio = (float) preSize.width / (float) preSize.height;
			float diff = Math.abs(picRatio - ratio);

			if (i == 0 || diff < closestDiff || (diff == closestDiff && preSize.width > closestWidth))
			{
				closestIdx = i;
				closestDiff = diff;
				closestWidth = preSize.width;
			}
		}

		Camera.Size preSize = presizes.get(closestIdx);
		currentParameters.setPreviewSize(preSize.width, preSize.height);
		currentParameters.setPictureSize(picSize.width, picSize.height);
		SurfaceTexture text = getSurfaceTexture();

		if (text != null)
			text.setDefaultBufferSize(preSize.width, preSize.height);

		List<String> focii = currentParameters.getSupportedFocusModes();
		sz = focii.size();
		String focusSelection = null;
		for (int i = 0; i < sz; i++)
		{
			String focus = focii.get(i);

			if (focus.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
			{
				focusSelection = focus;
				break;
			}
			if (focus.equals(Camera.Parameters.FOCUS_MODE_AUTO))
				focusSelection = focus;
		}

		if (focusSelection == null)
			focusSelection = focii.get(0);
		currentParameters.setFocusMode(focusSelection);

		camera.setParameters(currentParameters);
	}

	public void startPreview()
	{
		new Handler(Looper.getMainLooper()).post(new Runnable() {public void run()
		{
			try
			{
				SurfaceTexture texture = PreviewWidget.this.getSurfaceTexture();

				synchronized (lock)
				{
					if (camera == null || !previewReady || previewStarted)
						return;

					previewStarted = true;
				}

				camera.setPreviewTexture(texture);
				setupTexture(camera);
				updateTransform();
			}
			catch(IOException e){
				camera.release();
				camera = null;
				return;
			}

			camera.startPreview();

			if (listener != null)
				listener.onPreviewStarted();
		}});
	}

	public void stopPreview()
	{
		if (camera == null)
			return;

		releaseCamera();
		camera = null;

		synchronized (lock)
		{
			previewStarted = false;
		}

		if (listener != null)
			listener.onPreviewStopped();
	}

	private int getCameraRotation()
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

		return result;
	}

	public interface Listener
	{
		void onZoom(float zoom);
		void onPreviewStarted();
		void onPreviewStopped();
	}

	private Listener listener;

	public void setListener(Listener listener)
	{
		this.listener = listener;
	}

	public void setZoom(float zoom)
	{
		zoom = Math.max(1.0f, zoom);
		this.zoom = zoom;
		updateTransform();

		if (listener != null)
			listener.onZoom(zoom);
	}

	public float getZoom()
	{
		return zoom;
	}

	public void setOrientation(PhotoOrientation orientation)
	{
		this.orientation = orientation;
		updateTransform();
	}

	public PhotoOrientation getOrientation()
	{
		return orientation;
	}

	public boolean isFrontFacing()
	{
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
	}

	private void releaseCamera()
	{
		synchronized (lock)
		{
			if (camera == null)
				return;

			camera.stopPreview();
			camera.release();
			camera = null;
			currentParameters = null;
			previewStarted = false;
		}

	}

	public void setAndStartCamera(final boolean facing)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
		{
			MainActivity.getInstance().requestPermissionForResult(Manifest.permission.CAMERA, (result) -> {
				setAndStartCamera(facing);
			});
			return;
		}

		this.facing = facing;
		if (camera != null && isFrontFacing() == facing)
			return;

		releaseCamera();

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

		int tryLimit = 3;
		for (int tries = 0; tries < tryLimit; tries++)
		{
			try
			{
				Camera camera = Camera.open(cameraId);
				setCamera(camera);
				break;
			}
			catch (RuntimeException e)
			{
 				if (tries < tryLimit - 1)
				{
					try {Thread.sleep(500);} catch(InterruptedException e1){}
				}
				else
				{
					new Handler(Looper.getMainLooper()).post(new Runnable()
					{
						public void run()
						{
							Toast.makeText(getContext(), R.string.camera_open_error, Toast.LENGTH_LONG).show();
							MainActivity.getInstance().popSubViews();
						}
					});
				}
			}
		}
	}

	private Pair getCameraResolution(int camNum, Camera camera)
	{
		Camera.Parameters params = camera.getParameters();
		float focalLength = params.getFocalLength();
		float horizontalViewAngle = params.getHorizontalViewAngle();
		float verticalViewAngle = params.getVerticalViewAngle();
		float hAngle = (float) Math.toRadians(horizontalViewAngle);
		float vAngle = (float) Math.toRadians(verticalViewAngle);

		Pair ret = new Pair();
		ret.w = (float) (Math.tan(hAngle * 0.5f) * 2.0f * focalLength);
		ret.h = (float) (Math.tan(vAngle * 0.5f) * 2.0f * focalLength);
		return ret;
	}

	public boolean getFacing()
	{
		return facing;
	}

	public Camera getCamera()
	{
		return camera;
	}

	public String getCameraId()
	{
		return Integer.toString(cameraId);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		updateTransform();
	}

	private class Pair
	{
		public float w;
		public float h;
	}

	private void updateTransform()
	{
		if (camera == null)
			return;

		final Pair dims = new Pair();
		dims.h = getMeasuredHeight();
		dims.w = getMeasuredWidth();

		if (dims.h == 0 || dims.w == 0)
		{
			dims.h = getBottom() - getTop();
			dims.w = getRight() - getLeft();

			if (dims.h == 0 || dims.w == 0)
				return;
		}

		Handler h = getHandler();
		if (h == null)
			return;

		h.post(new Runnable() { public void run()
		{
			Matrix transform = new Matrix();
			cameraRotate(transform, dims);
			cameraDistort(transform, dims);
			zoomPreview(transform, dims);

			setTransform(transform);
		}});
	}

	private void cameraRotate(Matrix transform, Pair d)
	{
		float halfw = d.w * 0.5f;
		float halfh = d.h * 0.5f;

		transform.postTranslate(-halfw, -halfh);
		int degrees = getCameraRotation();
		transform.postRotate((float) degrees);
		transform.postTranslate(halfw, halfh);
	}

	private void cameraDistort(Matrix transform, Pair d)
	{
		if (currentParameters == null)
			return;


		Camera.Size sz = currentParameters.getPreviewSize();
		float ratio = (float) sz.height / (float) sz.width;

		if (ratio < 1.0f)
			ratio = 1.0f / ratio;

		int degrees = getCameraRotation();
		if (degrees == 90 || degrees == 270)
		{
			float targetHeight = d.w * ratio;
			ratio = targetHeight / d.h;

			float margin = d.h * 0.5f * (ratio - 1.0f);

			transform.postScale(1.0f, ratio);
			transform.postTranslate(0.0f, -margin);
		}
		else
		{
			float targetWidth = d.h * ratio;
			ratio = targetWidth / d.w;

			float margin = d.w * 0.5f * (ratio - 1.0f);

			transform.postScale(ratio, 1.0f);
			transform.postTranslate(-margin, 0.0f);
		}
	}

	private void zoomPreview(Matrix transform, Pair d)
	{
		float w = d.w * zoom;
		float h = d.h * zoom;
		float marginw = (w - d.w) * 0.5f;
		float marginh = (h - d.h) * 0.5f;

		transform.postScale(zoom, zoom);
		transform.postTranslate(-marginw, -marginh);
	}

	class ScaleDetector implements  ScaleGestureDetector.OnScaleGestureListener
	{
		private float zoom;
		private float startZoom;

		@Override
		public boolean onScale(ScaleGestureDetector scaleGestureDetector)
		{
			float factor = scaleGestureDetector.getScaleFactor();
			float delta = startZoom * (factor - 1.0f);
			float newZoom = zoom + delta;
			Log.d("stereoCamera", "new zoom: " + newZoom + " factor: " + factor);

			newZoom = Math.max(1.0f, Math.min(4.0f, newZoom));

			if (zoom != newZoom)
			{
				zoom = newZoom;
				setZoom(zoom);
			}

			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
		{
			startZoom = zoom;

			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector scaleGestureDetector)
		{
		}
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
				imageListener.onImage(bytes);

			camera.startPreview();
			imageListener.onFinished();

			imageListener = null;
		}
	};

	public interface ImageListener
	{
		void onImage(byte[] bytes);
		void onFinished();
	}

	protected ImageListener imageListener;
	protected long shutterStart;
	protected long shutterEnd;

	public enum ShutterType
	{
		HI_RES,
		LO_RES,
		PREVIEW
	};

	public void takePicture(ShutterType type, ImageListener imageListener)
	{
		this.imageListener = imageListener;
		shutterStart = System.currentTimeMillis();

		if (type == ShutterType.PREVIEW)
			takePreviewPicture();
		else
			getCamera().takePicture(shutterCallback, null, null, jpegCallback);

	}

	private void takePreviewPicture()
	{
		getHandler().post(new Runnable()
		{
			@Override
			public void run()
			{
				shutterCallback.onShutter();
				byte[] jpegData = getPreviewFrame();
				imageListener.onImage(jpegData);
				imageListener.onFinished();
				imageListener = null;
			}
		});
	}

	public byte[] getPreviewFrame()
	{
		return getPreviewFrame(-1);
	}

	public byte[] getPreviewFrame(int targetHeight)
	{
		if (currentParameters == null)
			return null;

		Camera.Size sz = currentParameters.getPreviewSize();
		float ratio = (float) sz.width / (float) sz.height;

		int targetW;

		if (targetHeight > 0)
			targetW = (int) ((float) targetHeight * ratio);
		else
		{
			targetHeight = sz.height;
			targetW = sz.width;
		}

		Bitmap bmp = Bitmap.createBitmap(targetW, targetHeight, Bitmap.Config.ARGB_8888);
		getBitmap(bmp);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
		final byte[] ret = baos.toByteArray();

		return ret;
	}
}

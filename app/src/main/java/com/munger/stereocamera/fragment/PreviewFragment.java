package com.munger.stereocamera.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Surface;
import android.view.View;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
import com.munger.stereocamera.widget.PreviewWidget;

public class PreviewFragment extends Fragment
{
	protected View rootView;
	protected PreviewWidget previewView;

	private Status status;

	protected void setStatus(Status status)
	{
		this.status = status;
	}

	public enum Status
	{
		NONE,
		CREATED,
		LISTENING,
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
	}

	protected void onCreateView(View rootView)
	{
		this.rootView = rootView;
		previewView = rootView.findViewById(R.id.preview);

		previewView.setListener(previewListener);
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState)
	{

	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		previewView.cleanUp();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putFloat("currentZoom", previewView.getZoom());
		outState.putBoolean("facing", previewView.getFacing());
		outState.putInt("status", status.ordinal());

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onViewStateRestored(@Nullable Bundle savedInstanceState)
	{
		super.onViewStateRestored(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.containsKey("status"))
		{
			Status oldStatus = Status.values()[savedInstanceState.getInt("status")];

			if (oldStatus == Status.BUSY || oldStatus == Status.READY)
			{
				previewView.setZoom(savedInstanceState.getFloat("currentZoom"));
				previewView.setOrientation(getCurrentOrientation());
				previewView.setAndStartCamera(savedInstanceState.getBoolean("facing"));
			}
		}
		else
		{}
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
					previewView.startPreview();
				}
			});
			return;
		}

		previewView.startPreview();
	}

	protected void setZoom(float zoom)
	{
		previewView.setZoom(zoom);
	}

	public PhotoOrientation getCurrentOrientation()
	{
		int rotation = MyApplication.getInstance().getCurrentActivity().getWindowManager().getDefaultDisplay().getRotation();

		switch (rotation)
		{
			case Surface.ROTATION_0: return PhotoOrientation.DEG_0;
			case Surface.ROTATION_90: return PhotoOrientation.DEG_90;
			case Surface.ROTATION_180: return PhotoOrientation.DEG_180;
			case Surface.ROTATION_270: return PhotoOrientation.DEG_270;
			default: return PhotoOrientation.DEG_0;
		}
	}

	public float getZoomValue()
	{
		return previewView.getZoom();
	}

	protected PreviewWidget.Listener previewListener = new PreviewWidget.Listener()
	{
		@Override
		public void onZoom(float zoom)
		{
			onZoomed();
		}

		@Override
		public void onPreviewStarted()
		{
			PreviewFragment.this.onPreviewStarted();
		}

		@Override
		public void onPreviewStopped()
		{
			PreviewFragment.this.onPreviewStopped();
		}
	};

	protected void onZoomed()
	{}

	protected void onPreviewStarted()
	{
		setStatus(Status.READY);
	}

	protected void onPreviewStopped()
	{
		setStatus(Status.BUSY);
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
		previewView.getCamera().takePicture(shutterCallback, null, null, jpegCallback);
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
			previewView.getCamera().takePicture(shutterCallback,null, null, pictureCallback);
		}
		catch(RuntimeException e){
			listener.fail();
		}
	}

	public void setFacing(boolean facing)
	{
		previewView.setAndStartCamera(facing);
	}

	public boolean getFacing()
	{
		return previewView.getFacing();
	}
}

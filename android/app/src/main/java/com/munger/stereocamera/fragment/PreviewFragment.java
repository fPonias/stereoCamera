package com.munger.stereocamera.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.widget.LoadingWidget;
import com.munger.stereocamera.widget.PreviewWidget;

public class PreviewFragment extends Fragment
{
	protected View rootView;
	protected PreviewWidget previewView;
	protected Handler handler;

	protected Status status;

	protected void setStatus(Status status)
	{
		this.status = status;

		switch (status)
		{
			case BUSY:
				setLoadingMessage("Camera busy");
				showLoading(true);
				break;
			case CREATED:
			case RESUMED:
				setLoadingMessage("Preview created");
				showLoading(true);
				break;
			case LISTENING:
				setLoadingMessage("Setting up communication");
				showLoading(true);
				break;
			case PROCESSING:
				setLoadingMessage("Processing photo data");
				showLoading(true);
				break;
			case READY:
				setLoadingMessage("Ready");
				showLoading(false);
				break;
		}
	}

	public enum Status
	{
		NONE,
		CREATED,
		RESUMED,
		LISTENING,
		PROCESSING,
		READY,
		BUSY
	};

	public PreviewFragment()
	{
		handler = new Handler(Looper.getMainLooper());
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
	public void onPause()
	{
		super.onPause();

		showLoading(false);
		previewView.stopPreview();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if (previewView != null)
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
			setStatus(oldStatus);

			if (oldStatus == Status.BUSY || oldStatus == Status.READY)
			{
				previewView.setZoom(savedInstanceState.getFloat("currentZoom"));
				previewView.setOrientation(MainActivity.getInstance().getCurrentOrientation());
				previewView.setAndStartCamera(savedInstanceState.getBoolean("facing"));
			}
		}
		else
		{}
	}

	protected void startPreview()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
		{
			MainActivity.getInstance().requestPermissionForResult(Manifest.permission.CAMERA, new MainActivity.PermissionResultListener()
			{
				@Override
				public void onResult(int resultCode)
				{
					previewView.startPreview();
				}
			});
			return;
		}

		previewView.startPreview();
	}

	protected void stopPreview()
	{
		previewView.stopPreview();
	}

	protected void setZoom(float zoom)
	{
		previewView.setZoom(zoom);
	}

	protected PreviewWidget.Listener previewListener = new PreviewWidget.Listener()
	{
		@Override
		public void onZoom(float zoom)
		{

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


	protected void onPreviewStarted()
	{
	}

	protected void onPreviewStopped()
	{
		setStatus(Status.BUSY);
	}


	public void fireShutter(PreviewWidget.SHUTTER_TYPE type, PreviewWidget.ImageListener listener)
	{
		previewView.takePicture(type, listener);
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
			previewView.getCamera().takePicture(shutterCallback,null, null , pictureCallback);
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

	public String getCameraId()
	{
		return previewView.getCameraId();
	}

	protected LoadingWidget loadingWidget;
	protected String loadingMessage = "";

	protected void setLoadingMessage(String message)
	{
		this.loadingMessage = message;

		if (loadingWidget != null)
			loadingWidget.setText(message);
	}

	protected void showLoading(final boolean loading)
	{
		if (!(rootView instanceof ViewGroup))
			return;

		handler.post(new Runnable() { public void run()
		{
			ViewGroup root = (ViewGroup) rootView;

			if (loading)
			{
				Context c = getContext();
				if (c == null)
					return;

				if (loadingWidget == null)
					loadingWidget = new LoadingWidget(getContext());

				loadingWidget.start();
				loadingWidget.setText(loadingMessage);

				if (root.indexOfChild(loadingWidget) == -1)
				{
					if (loadingWidget.getParent() != null)
						((ViewGroup)loadingWidget.getParent()).removeView(loadingWidget);

					root.addView(loadingWidget);
				}
			}
			else
			{
				if (loadingWidget == null)
					return;

				if (root.indexOfChild(loadingWidget) > -1)
				{
					loadingWidget.stop();
					root.removeView(loadingWidget);
				}
			}
		}});
	}
}

package com.munger.stereocamera.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.Flip;
import com.munger.stereocamera.bluetooth.command.master.GetAngleOfView;
import com.munger.stereocamera.bluetooth.command.master.Ping;
import com.munger.stereocamera.bluetooth.command.master.SetZoom;
import com.munger.stereocamera.bluetooth.utility.FireShutter;
import com.munger.stereocamera.bluetooth.utility.GetLatency;
import com.munger.stereocamera.bluetooth.utility.PhotoFiles;
import com.munger.stereocamera.bluetooth.utility.WaitForReadyStatus;
import com.munger.stereocamera.orientation.MasterOrientationCtrl;
import com.munger.stereocamera.orientation.OrientationCtrl;
import com.munger.stereocamera.orientation.OrientationWidget;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class MasterFragment extends PreviewFragment
{
	private ImageButton clickButton;
	private ImageButton flipButton;
	private ImageView thumbnailView;
	private OrientationWidget horizIndicator;

	private BluetoothMasterComm masterComm;
	private MasterOrientationCtrl gravityCtrl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		rootView = inflater.inflate(R.layout.fragment_master, container, false);
		super.onCreateView(rootView);

		clickButton = rootView.findViewById(R.id.shutter);
		flipButton = rootView.findViewById(R.id.flip);
		thumbnailView = rootView.findViewById(R.id.thumbnail);
		horizIndicator = rootView.findViewById(R.id.horiz_level);

		MyApplication.getInstance().getOrientationCtrl().setChangeListener(new OrientationCtrl.ChangeListener()
		{
			@Override
			public void change(float[] values)
			{
				float zval = values[0] * values[0] + values[1] * values[1];
				double azrad = Math.atan2(Math.sqrt(zval), values[2]);
				double az = azrad * 180.0 / Math.PI;

				horizIndicator.setRotation((float) az - 90.0f);
			}
		});

		thumbnailView.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			openThumbnail();
		}});

		clickButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				new FireShutter(MasterFragment.this).execute(localLatency, remoteLatency);
			}
		});

		flipButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				doFlip();
				masterComm.runCommand(new Flip(new Flip.Listener()
				{
					@Override
					public void done()
					{
					}

					@Override
					public void fail()
					{
					}
				}));
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
		final PhotoFiles photoFiles = new PhotoFiles();
		photoFiles.openTargetDir(new PhotoFiles.Listener()
		{
			@Override
			public void done()
			{
				new Handler(Looper.getMainLooper()).post(new Runnable() { public void run()
				{
					String max = photoFiles.getNewestFile().getPath();

					if (max == null)
					{
						thumbnailView.setImageDrawable(null);
						return;
					}
					else
					{
						Bitmap bmp = BitmapFactory.decodeFile(photoFiles.getFilePath(max));
						thumbnailView.setImageBitmap(bmp);
					}
				}});
			}
		});
	}

	private void onPreviewStarted1()
	{
		if (remoteLatency != -1 && localLatency != -1)
			return;

		Log.d(getTag(), "pinging slave phone");
		masterComm.runCommand(new Ping(new Ping.Listener()
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
				Toast.makeText(MyApplication.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
				((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
			}
		}));
	}

	private void onPreviewStarted2()
	{
		new WaitForReadyStatus(new WaitForReadyStatus.Listener()
		{
			@Override
			public void done(int status)
			{
				onPreviewStarted3();
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "wait for ready failed");
				Toast.makeText(MyApplication.getInstance(), R.string.bluetooth_communication_failed_error, Toast.LENGTH_LONG).show();
				((MainActivity) MyApplication.getInstance().getCurrentActivity()).popSubViews();
			}
		}, 3000).execute();

	}

	private void onPreviewStarted3()
	{
		masterComm.runCommand(new GetAngleOfView(new GetAngleOfView.Listener()
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
		}));
	}

	private void onPreviewStarted4(double zoom)
	{
		masterComm.runCommand(new SetZoom((float) zoom, new SetZoom.Listener()
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
		}));
	}

	final private Object lock = new Object();
	private boolean shutterFiring = false;

	private void onPreviewStarted5()
	{
		synchronized (lock)
		{
			if (shutterFiring)
				try{lock.wait();}catch(InterruptedException e){return;}

			shutterFiring = true;
		}

		new GetLatency(new GetLatency.Listener()
		{
			@Override
			public void trigger(final GetLatency.ListenerListener listener)
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
		}, 3000).execute();

		synchronized (lock)
		{
			if (shutterFiring)
			{
				try {lock.wait(2000); } catch(InterruptedException e){}
				shutterFiring = false;
			}
		}

		onPreviewStarted6();
	}

	private void onPreviewStarted6()
	{
		gravityCtrl = new MasterOrientationCtrl(horizIndicator);
		gravityCtrl.start();
	}

	private long localLatency = -1;
	private long remoteLatency = -1;



	private void openThumbnail()
	{
		BaseActivity act = MyApplication.getInstance().getCurrentActivity();
		if (act instanceof MainActivity)
			((MainActivity) act).startThumbnailView();
	}
}

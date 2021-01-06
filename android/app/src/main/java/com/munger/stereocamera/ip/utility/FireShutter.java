package com.munger.stereocamera.ip.utility;

import android.net.Uri;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.fragment.MasterFragment;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.commands.SendPhoto;
import com.munger.stereocamera.service.ImagePair;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.widget.PreviewWidget;

import java.io.File;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean shutterFiring = false;
	private Thread fireThread = null;

	private ImagePair.ImageArg localData;
	private ImagePair.ImageArg remoteData;
	private boolean flip = false;

	private CommCtrl masterComm;
	private MasterFragment fragment;

	public FireShutter(MasterFragment fragment)
	{
		this.fragment = fragment;
		masterComm = MyApplication.getInstance().getCtrl();
		photoFiles = PhotoFiles.Factory.get();

		fireThread = new Thread(this::fireLocal);
		fireThread.setPriority(Thread.MAX_PRIORITY);
		isRunning = true;
		fireThread.start();
	}

	public void cleanUp()
	{
		synchronized (lock)
		{
			isRunning = false;
			lock.notify();
		}
	}

	public interface Listener
	{
		void onProcessing();
		void done();
		void fail();
	}

	private Listener listener;

	public void execute(final long delay, PreviewWidget.SHUTTER_TYPE type, final Listener listener)
	{
		synchronized (lock)
		{
			if (shutterFiring)
				return;

			fireType = type;
			wait = delay;

			this.listener = listener;
			shutterFiring = true;

			lock.notify();
		}

		fireRemote(type);
	}

	private PreviewWidget.SHUTTER_TYPE fireType;
	private long wait;
	private boolean isRunning = false;

	private void fireLocal()
	{
		while (isRunning)
		{
			synchronized (lock)
			{
				if (!shutterFiring)
					try {lock.wait();} catch(InterruptedException ignored){};

				if (!isRunning)
					return;
			}

			fireLocal2(fireType, wait);
		}
	}

	private void fireLocal2(final PreviewWidget.SHUTTER_TYPE type, final long wait)
	{
		synchronized (lock)
		{
			localData = new ImagePair.ImageArg();
		}

		if (wait > 0)
			try { Thread.sleep(wait); } catch(InterruptedException e){}

		fragment.fireShutter(type, new PreviewWidget.ImageListener()
		{
			@Override
			public void onImage(byte[] bytes)
			{
				synchronized (lock)
				{
					if (!isRunning)
						return;

					String localPath = photoFiles.saveDataToCache(bytes);
					localData.path = localPath;
				}

				handleLocal();
			}

			@Override
			public void onFinished()
			{}
		});

		synchronized (lock)
		{
			localData.orientation = MainActivity.getInstance().getCurrentOrientation();
			localData.zoom = fragment.getZoom();
		}

		handleLocal();

		MyApplication.getInstance().getPrefs().setLocalZoom(fragment.getCameraId(), fragment.getZoom());
	}

	private void handleLocal()
	{
		boolean doNext = false;

		synchronized (lock)
		{
			if (localData.path == null || localData.orientation == null)
				return;

			localShutterDone = true;

			if (remoteShutterDone)
				doNext = true;

			shutterFiring = false;
			lock.notify();
		}

		if (doNext)
			done();
	}

	private void fireRemote(PreviewWidget.SHUTTER_TYPE type)
	{
		masterComm.sendCommand(new com.munger.stereocamera.ip.command.commands.FireShutter(), new CommCtrl.ResponseListener()
		{
			@Override
			public void onReceived(Command command, Command origCommand)
			{
				com.munger.stereocamera.ip.command.commands.FireShutter r = (com.munger.stereocamera.ip.command.commands.FireShutter) command;
				remoteData = new ImagePair.ImageArg();
				remoteData.orientation = r.orientation;
				remoteData.zoom = r.zoom;

				String remotePath = photoFiles.saveDataToCache(r.data);
				remoteData.path = remotePath;

				MyApplication.getInstance().getPrefs().setRemoteZoom(fragment.getCameraId(), r.zoom);

				boolean doNext = false;

				synchronized (lock)
				{
					if (!isRunning)
						return;

					remoteShutterDone = true;

					if (localShutterDone)
						doNext = true;
				}

				if (doNext)
					done();
			}

			@Override
			public void onReceiveFailed(Command command)
			{
				remoteData = null;
				boolean doNext = false;

				synchronized (lock)
				{
					if (!isRunning)
						return;

					remoteShutterDone = true;

					if (localShutterDone)
						doNext = true;
				}

				if (doNext)
					done();
			}
		}, 30000);
	}

	private PhotoFiles photoFiles;

	private void done()
	{
		listener.onProcessing();
		photoFiles = PhotoFiles.Factory.get();
		if (localData == null || remoteData == null)
		{
			listener.fail();
			return;
		}

		boolean isOnLeft = MyApplication.getInstance().getPrefs().getIsOnLeft();

		if (!isOnLeft)
		{
			ImagePair.ImageArg tmp = remoteData;
			remoteData = localData;
			localData = tmp;
		}

		done2();
	}

	private void done2()
	{
		Preferences prefs = MyApplication.getInstance().getPrefs();
		ImagePair args = new ImagePair();
		args.flip = prefs.getIsFacing();
		args.type = PhotoProcessor.CompositeImageType.SPLIT;
		args.left = localData;
		args.right = remoteData;
	}
}

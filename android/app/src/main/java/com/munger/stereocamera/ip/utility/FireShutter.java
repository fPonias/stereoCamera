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
import com.munger.stereocamera.service.PhotoProcessorWorker;
import com.munger.stereocamera.utility.PhotoFiles;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.utility.SingleThreadedExecutor;
import com.munger.stereocamera.widget.PreviewWidget;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class FireShutter
{
	private final Object lock = new Object();
	private boolean localShutterDone = false;
	private boolean remoteShutterDone = false;
	private boolean fireShutter = false;
	private SingleThreadedExecutor fireThread;

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


		fireThread = new SingleThreadedExecutor("Fire Shutter queue");
		fireThread.setPriority(Thread.MAX_PRIORITY);
		isRunning = false;
	}

	public void cleanUp()
	{
		fireThread.stop();
	}

	public interface Listener
	{
		void remoteReceived(ImagePair.ImageArg arg);
		void localReceived(ImagePair.ImageArg arg);
		void onProcessing();
		void done();
		void fail();
	}

	private ExecuteTask currentTask;

	private class ExecuteTask implements Runnable
	{
		public PreviewWidget.ShutterType type;
		public long wait;
		public Listener listener;

		public ExecuteTask(PreviewWidget.ShutterType type, long wait, Listener listener)
		{
			this.type = type;
			this.wait = wait;
			this.listener = listener;
		}

		public void run()
		{
			currentTask = this;
			fireLocal();
			fireRemote();
		}
	}

	public void execute(long delay, PreviewWidget.ShutterType type, Listener listener)
	{
		synchronized (lock)
		{
			if (isRunning)
				return;

			isRunning = true;
		}

		ExecuteTask task = new ExecuteTask(type, delay, listener);
		fireThread.execute(task);
	}

	private boolean isRunning = false;

	private void fireLocal()
	{
		synchronized (lock)
		{
			localData = new ImagePair.ImageArg();
		}

		if (currentTask.wait > 0)
			try { Thread.sleep(currentTask.wait); } catch(InterruptedException e){}

		fragment.fireShutter(currentTask.type, new PreviewWidget.ImageListener()
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
	}

	private void handleLocal()
	{
		boolean doNext = false;
		currentTask.listener.localReceived(localData);

		synchronized (lock)
		{
			if (localData.path == null || localData.orientation == null)
				return;

			localShutterDone = true;

			if (remoteShutterDone)
				doNext = true;

			fireShutter = false;
			lock.notify();
		}

		if (doNext)
			done();
	}

	private void fireRemote()
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

				currentTask.listener.remoteReceived(remoteData);

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
		currentTask.listener.onProcessing();
		photoFiles = PhotoFiles.Factory.get();
		if (localData == null || remoteData == null)
		{
			currentTask.listener.fail();
			return;
		}

		if (!fragment.getIsOnLeft())
		{
			ImagePair.ImageArg tmp = remoteData;
			remoteData = localData;
			localData = tmp;
		}

		synchronized (lock)
		{
			isRunning = false;
		}

		done2();
	}

	private void done2()
	{
		ImagePair args = new ImagePair();
		args.flip = fragment.getFacing();
		args.left = localData;
		args.right = remoteData;

		PhotoProcessorWorker.RunListener workerListener = new PhotoProcessorWorker.RunListener(fragment.getViewLifecycleOwner())
		{
			@Override
			public void onResult(Uri uri, int id) {
				SendPhoto cmd = new SendPhoto(id);
				masterComm.sendCommand(cmd, (success, command, originalCmd) -> {

					currentTask.listener.done();
				});
			}
		};

		currentTask.listener.onProcessing();
		Preferences prefs = new Preferences();
		prefs.setup();
		Set<PhotoProcessor.CompositeImageType> types = prefs.getProcessorTypes();
		int count = types.size();
		int i = 0;
		for (PhotoProcessor.CompositeImageType type : types)
		{
			args.type = type;
			boolean clean = (i == count - 1);

			UUID procId = MainActivity.getInstance().photoProcessorWorker.run(args, clean);

			if (clean)
			{
				MainActivity.getInstance().runOnUiThread(() -> {
					MainActivity.getInstance().photoProcessorWorker.listen(procId, workerListener);
				});
			}

			i++;
		}
	}
}

package com.munger.stereocamera.fragment;

import android.util.Log;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.command.Command;
import com.munger.stereocamera.ip.command.commands.Handshake;
import com.munger.stereocamera.ip.command.commands.ID;
import com.munger.stereocamera.ip.command.commands.Ping;
import com.munger.stereocamera.ip.command.commands.SetCaptureQuality;
import com.munger.stereocamera.ip.command.commands.SetFacing;
import com.munger.stereocamera.ip.command.commands.SetOverlay;
import com.munger.stereocamera.ip.command.commands.SetZoom;
import com.munger.stereocamera.ip.command.commands.Version;
import com.munger.stereocamera.ip.utility.RemoteState;
import com.munger.stereocamera.widget.PreviewOverlayWidget;

import java.util.ArrayList;

public class MasterShake
{
	private boolean handshaking = false;
	private int currentStep = 0;
	private final Object lock = new Object();
	private Listener finalListener;
	private StepListener stepListener;

	private final long PING_TIMEOUT = 3000;
	private final long HANDSHAKE_TIMEOUT = 1000;
	private final long LISTEN_STATUS_TIMEOUT = 10000;
	private final long READY_STATUS_TIMEOUT = 6000;

	private MasterFragment target;
	private CommCtrl comm;

	public MasterShake(MasterFragment target)
	{
		Log.d("stereoCamera", "new handshake created");

		this.target = target;

		init();

		steps.add(pingStep);
		steps.add(handshakeStep);
		steps.add(versionCheckStep);
		steps.add(idStep);
		steps.add(setCameraStep);
		steps.add(setQualityStep);
		steps.add(setZoomStep);
		steps.add(overlayStep);
		steps.add(readyStep);
	}

	public void start(Listener listener)
	{
		synchronized (lock)
		{
			if (handshaking)
				return;

			handshaking = true;
		}

		finalListener = listener;
		stepListener = new StepListener()
		{
			@Override
			public void success()
			{
				Log.d("stereoCamera", "step success");
				nextStep();
			}

			@Override
			public void fail()
			{
				Log.d("stereoCamera", "step fail");
				runFail();
			}
		};

		currentStep = -1;
		nextStep();
	}

	private void nextStep()
	{
		currentStep++;

		if (currentStep == steps.size())
		{
			synchronized (lock)
			{
				handshaking = false;
			}

			Log.d("stereoCamera", "handshake success");
			finalListener.success();
			return;
		}

		Step step = steps.get(currentStep);
		Log.d("stereoCamera", "executing handshake step " + currentStep + " : " + step.name);
		step.execute(stepListener);
	}

	private void runFail()
	{
		synchronized (lock)
		{
			handshaking = false;
		}

		finalListener.fail();
	}

	private ArrayList<Step> steps = new ArrayList<>();

	public interface Listener
	{
		void success();
		void fail();
	}

	private interface StepListener
	{
		void success();
		void fail();
	}

	private abstract class Step
	{
		public String name;

		public Step(String name)
		{
			this.name = name;
		}

		public abstract void execute(StepListener listener);
	}

	private String getTag()
	{
		return "Master shake";
	}

	private Step pingStep;
	private Step versionCheckStep;
	private Step setCameraStep;
	private Step overlayStep;
	private Step handshakeStep;
	private Step idStep;
	private Step setQualityStep;
	private Step setZoomStep;
	private Step readyStep;

	private void init()
	{
		comm = MainActivity.getInstance().getCtrl();

		pingStep = new Step("ping") { public void execute(final StepListener listener)
		{
			Log.d("stereoCamera", "pinging slave phone");
			comm.sendCommand(new Ping(), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() { public void r(boolean success, Command command, Command originalCmd)
			{
				if (success)
				{
					Log.d("stereoCamera", "ping success");
					listener.success();
				}
				else
					listener.fail();
			}}), PING_TIMEOUT);
		}};

		handshakeStep = new Step("handshake") { public void execute(final StepListener listener)
		{
			Log.d("stereoCamera", "initiating handshake");
			comm.sendCommand(new Handshake(), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() {
				@Override
				public void r(boolean success, Command command, Command originalCmd) {
				if (success)
				{
					Log.d("stereoCamera", "handshake started");
					listener.success();
				}
				else
					listener.fail();
			}}), HANDSHAKE_TIMEOUT);
		}};

		idStep = new Step("id") { public void execute(final StepListener listener)
		{
			Log.d("stereoCamera", "fetching id");
			comm.sendCommand(new ID(), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() {
				@Override
				public void r(boolean success, Command command, Command originalCmd) {
				if (success)
				{
					ID cmd = (ID) command;
					Log.d("stereoCamera", "remote camera ID " + cmd.phoneId);
					listener.success();
				}
				else
					listener.fail();
			}}), 3000);
		}};

		setQualityStep = new Step("quality") { public void execute(final StepListener listener)
		{
			Log.d("stereoCamera", "setting image quality");
			comm.sendCommand(new SetCaptureQuality(SetCaptureQuality.ImageQuality.LO_RES), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() {
				@Override
				public void r(boolean success, Command command, Command originalCmd) {
				if (success)
					listener.success();
				else
					listener.fail();
				}
			}), HANDSHAKE_TIMEOUT);
		}};

		setZoomStep = new Step("zoom") { public void execute(final StepListener listener) {
			Log.d("stereoCamera", "setting zoom value");
			comm.sendCommand(new SetZoom(1.0f), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() {
				@Override
				public void r(boolean success, Command command, Command originalCmd) {
					if (success)
						listener.success();
					else
						listener.fail();
				}
			}), HANDSHAKE_TIMEOUT);
		}};

		readyStep = new Step("listen ready") { public void execute(final StepListener listener)
			{
				PreviewFragment.Status[] statuss = new PreviewFragment.Status[]{PreviewFragment.Status.LISTENING, PreviewFragment.Status.READY};
				target.remoteState.waitOnStatusAsync(statuss, LISTEN_STATUS_TIMEOUT, new RemoteState.ReadyListener()
				{
					@Override
					public void done()
					{
						listener.success();
					}

					@Override
					public void fail()
					{
						listener.fail();
					}
				});
			}
		};

		versionCheckStep = new Step("check version") { public void execute(final StepListener listener)
		{
			comm.sendCommand(new Version(), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() { public void r(boolean success, Command command, Command originalCmd)
			{
				if (!success)
				{
					listener.fail();
					return;
				}

				Version resp = (Version) command;
				int remoteVersion = resp.version;

				if (resp.platform == Version.Platform.ANDROID && remoteVersion > ((Version)originalCmd).version)
					listener.fail();
				else if (resp.platform == Version.Platform.IOS && remoteVersion > 1000)
					listener.fail();
				else
					listener.success();
				}
			}), 1000);
		}};

		class SetupCameraStep extends Step
		{
			private boolean localFacing = false;
			private boolean remoteFacing = false;
			private final Object facingLock = new Object();
			private boolean listenerCalled = false;
			private long start;
			private StepListener listener;

			public SetupCameraStep(String name)
			{
				super(name);
			}

			public void execute(final StepListener listener)
			{
				start = System.currentTimeMillis();
				this.listener = listener;
				localFacing = false;
				remoteFacing = false;

				boolean isFacing = MainActivity.getInstance().getPrefs().getIsFacing();
				target.startPreview();
				target.setCamera(isFacing, new MasterFragment.SetCameraListener()
				{
					@Override
					public void done()
					{
						Log.d("stereoCamera", "set local camera finished");
						synchronized (facingLock)
						{
							localFacing = true;
						}

						execute2();
					}

					@Override
					public void fail()
					{
						Log.d("stereoCamera", "set local camera failed");
					}
				});

				comm.sendCommand(new SetFacing(isFacing), new CommCtrl.DefaultResponseListener(new CommCtrl.IDefaultResponseListener() { public void r(boolean success, Command command, Command originalCmd)
				{
					Log.d("stereoCamera", "set remote camera finished");
					if (success)
					{
						synchronized (facingLock)
						{
							remoteFacing = true;
						}

						execute2();
					}
				}}), 3000);

				Thread timeoutThread = new Thread(new Runnable() {public void run()
				{
					synchronized (facingLock)
					{
						if (!listenerCalled)
						{
							try{facingLock.wait(3000);} catch(InterruptedException e){}
						}

						if (!listenerCalled)
						{
							execute2();
						}
					}
				}});
				timeoutThread.start();
			}

			private void execute2()
			{
				boolean doSuccess = false;
				boolean doFail = false;
				synchronized (facingLock)
				{
					if (listenerCalled)
						return;

					if (localFacing && remoteFacing)
					{
						doSuccess = true;
						listenerCalled = true;
					}
					else if (System.currentTimeMillis() - start > 3000)
					{
						doFail = true;
						listenerCalled = true;
					}

					if (listenerCalled)
						facingLock.notify();
				}

				if (doSuccess)
					listener.success();
				else if (doFail)
					listener.fail();
			}
		}

		setCameraStep = new SetupCameraStep("setup camera");

		overlayStep = new Step("overlay") { public void execute(final StepListener listener)
		{
			final PreviewOverlayWidget.Type type = target.overlayWidget.getType();
			target.masterComm.sendCommand(new SetOverlay(type), null);

			target.remoteState.waitOnStatusAsync(PreviewFragment.Status.READY, READY_STATUS_TIMEOUT, new RemoteState.ReadyListener()
			{
				@Override
				public void done()
				{
					if (type == PreviewOverlayWidget.Type.Ghost)
						target.slavePreview.start();

					listener.success();
				}

				@Override
				public void fail()
				{
					Log.d(getTag(), "slave ready timed out");
					listener.fail();
				}
			});
		}};
	}
}

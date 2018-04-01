package com.munger.stereocamera.fragment;

import android.util.Log;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.bluetooth.command.master.commands.Handshake;
import com.munger.stereocamera.bluetooth.command.master.commands.Ping;
import com.munger.stereocamera.bluetooth.command.master.commands.SetOverlay;
import com.munger.stereocamera.bluetooth.utility.RemoteState;
import com.munger.stereocamera.bluetooth.utility.TimedCommand;
import com.munger.stereocamera.widget.PreviewOverlayWidget;

import java.util.ArrayList;

public class MasterHandshake
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

	public MasterHandshake(MasterFragment target)
	{
		Log.d(getTag(), "new handshake created");

		this.target = target;

		steps.add(pingStep);
		steps.add(createdStatusStep);
		steps.add(initStep);
		steps.add(listenStatusStep);
		steps.add(setCameraStep);
		steps.add(overlayStep);
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
				Log.d(getTag(), "step success");
				nextStep();
			}

			@Override
			public void fail()
			{
				Log.d(getTag(), "step fail");
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

			Log.d(getTag(), "handshake success");
			finalListener.success();
			return;
		}

		Step step = steps.get(currentStep);
		Log.d(getTag(), "executing handshake step " + currentStep + " : " + step.name);
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

	private Step pingStep = new Step("ping") { public void execute(final StepListener listener)
	{
		Log.d(getTag(), "pinging slave phone");
		TimedCommand tp = new TimedCommand(PING_TIMEOUT, new TimedCommand.Listener() { public void done(boolean success, MasterIncoming response)
		{
			if (success)
			{
				Log.d(getTag(), "ping success");
				listener.success();
			}
			else
				listener.fail();
		}});
		tp.run(new Ping());
	}};

	private Step initStep = new Step("init") { public void execute(final StepListener listener)
		{
			Log.d(getTag(), "initiating handshake to slave phone");
			TimedCommand tp = new TimedCommand(HANDSHAKE_TIMEOUT, new TimedCommand.Listener() { public void done(boolean success, MasterIncoming response)
			{
				if (success)
				{
					Log.d(getTag(), "handshake initiated on slave phone");
					listener.success();
				}
				else
					listener.fail();
			}});
			tp.run(new Handshake());
		}
	};

	private Step createdStatusStep = new Step("listen created") { public void execute(final StepListener listener)
	{
		PreviewFragment.Status[] statuss = new PreviewFragment.Status[]{PreviewFragment.Status.RESUMED, PreviewFragment.Status.CREATED};
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
	}};

	private Step listenStatusStep = new Step("listen ready") { public void execute(final StepListener listener)
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

	private Step setCameraStep = new Step("setup camera") { public void execute(final StepListener listener)
		{
			boolean isFacing = MyApplication.getInstance().getPrefs().getIsFacing();
			target.startPreview();
			target.setCamera(isFacing, new MasterFragment.SetCameraListener()
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

	private Step overlayStep = new Step("overlay") { public void execute(final StepListener listener)
		{
			final PreviewOverlayWidget.Type type = target.overlayWidget.getType();
			target.masterComm.runCommand(new SetOverlay(type), null);

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
		}
	};
}

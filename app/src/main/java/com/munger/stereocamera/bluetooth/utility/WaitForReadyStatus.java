package com.munger.stereocamera.bluetooth.utility;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.GetStatus;
import com.munger.stereocamera.fragment.PreviewFragment;

public class WaitForReadyStatus
{
	public Listener listener;
	private BluetoothMasterComm masterComm;

	public WaitForReadyStatus(Listener listener, long timeout)
	{
		this.listener = listener;
		this.slaveReadyTimeout = timeout;

		localListener = new GetStatus.Listener()
		{
			@Override
			public void done(int status)
			{
				localDone(status);
			}

			@Override
			public void fail()
			{
				WaitForReadyStatus.this.listener.fail();
			}
		};

		masterComm = MyApplication.getInstance().getBtCtrl().getMaster().getComm();
	}

	public interface Listener
	{
		void done(int status);
		void fail();
	}

	private long slaveReadyStart;
	private long slaveReadyTimeout;
	private GetStatus.Listener localListener;

	private void localDone(int status)
	{
		WaitState state = waitForSlaveReady_NextAction(status);

		switch(state)
		{
			case FAIL:
				listener.fail();
				break;
			case DO_OVER:
				masterComm.runCommand(new GetStatus(localListener));
				break;
			case SUCCESS:
				listener.done(status);
				break;
		}
	}

	public void execute()
	{
		slaveReadyStart = System.currentTimeMillis();
		masterComm.runCommand(new GetStatus(localListener));
	}

	private enum WaitState
	{
		DO_OVER,
		SUCCESS,
		FAIL
	}

	private WaitState waitForSlaveReady_NextAction(int status)
	{
		if (status == PreviewFragment.READY)
		{
			return WaitState.SUCCESS;
		}

		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e){
			return WaitState.FAIL;
		}

		long now = System.currentTimeMillis();
		long diff = now - slaveReadyStart;

		if (diff < slaveReadyTimeout)
			return WaitState.DO_OVER;
		else
			return WaitState.FAIL;
	}
}

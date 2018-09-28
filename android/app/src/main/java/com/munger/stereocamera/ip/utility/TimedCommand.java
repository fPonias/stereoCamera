package com.munger.stereocamera.ip.utility;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketCtrlCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothMaster;
import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.master.MasterIncoming;
import com.munger.stereocamera.ip.command.master.commands.MasterCommand;

/**
 * Created by hallmarklabs on 3/29/18.
 */

public class TimedCommand
{
	private Listener listener;

	private boolean returned = false;

	private long timeout;
	private MasterIncoming response;
	private boolean done = false;
	private final Object lock = new Object();
	private MasterComm masterComm;

	public interface Listener
	{
		void done(boolean success, MasterIncoming response);
	}

	public TimedCommand(long timeout, Listener listener)
	{
		this.timeout = timeout;
		this.listener = listener;
	}

	public void run(MasterCommand command)
	{
		response = null;

		SocketCtrlCtrl ctrl = MainActivity.getInstance().getCtrl();

		if (ctrl == null)
		{
			command.onExecuteFail();
			return;
		}

		SocketCtrl master = ctrl.getMaster();

		if (master == null)
		{
			command.onExecuteFail();
			return;
		}

		masterComm = ctrl.getMasterComm();

		if (masterComm == null)
		{
			command.onExecuteFail();
			return;
		}

		masterComm.runCommand(command, new MasterComm.SlaveListener()
		{
			@Override
			public void onResponse(MasterIncoming response)
			{
				synchronized (lock)
				{
					if (done)
						return;

					returned = true;
					done = true;
					TimedCommand.this.response = response;

					lock.notify();
				}
			}
		});

		Thread t = new Thread(new Runnable() { public void run()
		{
			synchronized (lock)
			{
				if (!done)
					try {lock.wait(timeout);} catch(InterruptedException e){}

				done = true;
			}

			listener.done(returned, response);
		}});
		t.start();
	}
}

package com.munger.stereocamera.bluetooth.utility;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.command.BluetoothCommands;
import com.munger.stereocamera.bluetooth.command.master.BluetoothMasterComm;
import com.munger.stereocamera.bluetooth.command.master.MasterIncoming;
import com.munger.stereocamera.bluetooth.command.master.commands.MasterCommand;
import com.munger.stereocamera.bluetooth.command.master.commands.Ping;

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
	private BluetoothMasterComm masterComm;

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

		BluetoothCtrl btCtrl = MainActivity.getInstance().getBtCtrl();

		if (btCtrl == null)
		{
			command.onExecuteFail();
			return;
		}

		BluetoothMaster master = btCtrl.getMaster();

		if (master == null)
		{
			command.onExecuteFail();
			return;
		}

		masterComm = master.getComm();

		if (masterComm == null)
		{
			command.onExecuteFail();
			return;
		}

		masterComm.runCommand(command, new BluetoothMasterComm.SlaveListener()
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

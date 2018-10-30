package com.munger.stereocamera.ip.command;

import android.util.Log;

import java.util.ArrayList;

public class Sender
{
	private CommCtrl ctrl;

	public Sender(CommCtrl ctrl)
	{
		this.ctrl = ctrl;
	}

	private final Object commandSenderCondition = new Object();
	private boolean commandSenderIsRunning = false;
	private ArrayList<Command> commandQueue = new ArrayList<>();


	public void sendCommand(Command command, CommCtrl.ResponseListener listener, long timeout)
	{
		if (!commandSenderIsRunning)
			return;

		if (listener != null)
		{
			CommCtrl.ResponseListenerStr str = new CommCtrl.ResponseListenerStr();
			str.command = command;
			str.listener = listener;
			str.timeout = timeout;
			ctrl.addResponseListener(str);
		}

		synchronized (commandSenderCondition)
		{
			commandQueue.add(command);
			Log.d("stereoCamera", "send: enqueueing command: " + command.cmdtype.name());
			commandSenderCondition.notify();
		}
	}

	public void startCommandSender()
	{
		if (commandSenderIsRunning)
			return;

		Thread t = new Thread(new Runnable() { public void run()
		{
			Thread.currentThread().setName("Command sender");

			if (commandSenderIsRunning)
				return;

			commandSenderIsRunning = true;

			Log.d("stereoCamera", "send: started");
			while (commandSenderIsRunning)
			{
				sendCommand();
			}
		}});
		t.start();
	}

	public void stopAll()
	{
		synchronized (commandSenderCondition)
		{
			commandSenderIsRunning = false;
			commandQueue.clear();
			commandSenderCondition.notify();
		}
	}

	private void sendCommand()
	{
		Command nextCommand = null;

		synchronized (commandSenderCondition)
		{
			if (!commandSenderIsRunning)
				return;

			if (commandQueue.size() == 0)
			{
				Log.d("stereoCamera", "send: waiting");
				try
				{
					commandSenderCondition.wait();
				}
				catch (InterruptedException e)
				{
					return;
				}
			}

			if (commandSenderIsRunning && commandQueue.size() > 0)
				nextCommand = commandQueue.remove(0);
		}

		if (nextCommand == null)
			return;

		ctrl.updateResponseListener(nextCommand);

		Log.d("stereoCamera", "send: sending command " + nextCommand.cmdtype.name());
		boolean success = nextCommand.send(ctrl.getComm());

		CommCtrl.ResponseListenerStr str = ctrl.getResponseListener(nextCommand);
		if (str != null)
		{
			if (!success)
				str.listener.onSendFailed(nextCommand);
			else
				str.listener.onSent(nextCommand);
		}
	}
}

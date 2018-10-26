package com.munger.stereocamera.ip.command;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class Receiver
{
	private CommCtrl ctrl;

	public Receiver(CommCtrl ctrl)
	{
		this.ctrl = ctrl;
	}

	private final Object commandsReceivedCnd = new Object();
	private ArrayList<Command> commandsReceivedQueue = new ArrayList<>();
	private final Object commandRepliesCnd = new Object();
	private ArrayList<Command> commandRepliesQueue = new ArrayList<>();

	public void startCommandReceiver()
	{
		if (ctrl.getCommandReceiverIsRunning())
			{return;}

		Thread t = new Thread(new Runnable() { public void run()
		{
			if (ctrl.getCommandReceiverIsRunning())
				{return;}

			Thread.currentThread().setName("Command Receiver");
			Log.d("stereoCamera", "starting command receiver");
			ctrl.setCommandReceiverIsRunning(true);

			startCommandsReceivedProcessor();
			startCommandRepliesProcessor();

			boolean success = true;
			while(success == true && ctrl.getCommandReceiverIsRunning())
			{
				success = receiveCommand();
			}

			if (!success)
			{
				CommCtrl.StateListener listener = ctrl.getListener();
				if (listener != null)
					listener.onDisconnect();

				ctrl.disconnect();
			}
		}});

		t.start();
	}

	public void stopAll()
	{
		if (ctrl.getCommandReceiverIsRunning())
			ctrl.setCommandReceiverIsRunning(false);

		synchronized (commandRepliesCnd)
		{
			commandRepliesQueue.clear();
			commandRepliesCnd.notifyAll();
		}

		synchronized (commandsReceivedCnd)
		{
			commandsReceivedQueue.clear();
			commandsReceivedCnd.notifyAll();
		}
	}

	private boolean receiveCommand()
	{
		Command.Type type;
		byte b = 0;

		try
		{
			b = ctrl.getComm().getByte();
			Command.Type[] types = Command.Type.values();

			if (b < 0 || b >= types.length)
			{
				throw new IOException("invalid command byte " + b + " received");
			}

			type = types[b];

			if (type == Command.Type.NONE)
			{
				throw new IOException("invalid command type NONE received");
			}
		}
		catch(IOException e){
			return false;
		}

		Log.d("stereoCamera","read command type " + type.name());
		Command cmd = CommandFactory.build(type);

		if (cmd.cmdtype == Command.Type.NONE)
			return false;

		boolean success = cmd.receive(ctrl.getComm());
		CommCtrl.ResponseListenerStr str = ctrl.getResponseListener(cmd);

		if (!success)
		{
			if (str != null)
				str.listener.onReceiveFailed(cmd);

			return false;
		}
		else
		{
			if (str != null)
				str.listener.onReceived(cmd, str.command);
		}

		if (cmd.isResponse)
		{
			Log.d("stereoCamera","reply: enqueueing response " + cmd.cmdtype.name());
			synchronized (commandRepliesCnd)
			{
				commandRepliesQueue.add(cmd);
				commandRepliesCnd.notify();
			}
		}
		else
		{
			Log.d("stereoCamera","received: enqueueing command " + cmd.cmdtype.name());
			synchronized (commandsReceivedCnd)
			{
				commandsReceivedQueue.add(cmd);
				commandsReceivedCnd.notify();
			}
		}

		return true;
	}

	private void startCommandRepliesProcessor()
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			Thread.currentThread().setName("Command Reply Receiver");
			while (ctrl.getCommandReceiverIsRunning())
			{
				processCommandReply();
			}
		}});
		t.start();
	}

	private void processCommandReply()
	{
		Command cmd = null;

		synchronized (commandRepliesCnd)
		{
			if (commandRepliesQueue.size() == 0)
			{
				Log.d("stereoCamera", "reply: response processor waiting");
				try {commandRepliesCnd.wait();} catch(InterruptedException e){ return; }
			}

			if (commandRepliesQueue.size() > 0)
			{
				cmd = commandRepliesQueue.remove(0);
			}
		}

		if (cmd == null)
			return;

		CommCtrl.ResponseListenerStr str = ctrl.getAndRemoveResponseListener(cmd);

		if (str != null)
		{
			Log.d("stereoCamera","reply: handling command " + cmd.cmdtype.name());
			str.command.onResponse(cmd);
		}
	}

	private void startCommandsReceivedProcessor()
	{

		Thread t = new Thread(new Runnable() { public void run()
		{
			Thread.currentThread().setName("Commands Received Receiver");
			while (ctrl.getCommandReceiverIsRunning())
			{
				processReceivedCommand();
			}
		}});
		t.start();
	}

	private void processReceivedCommand()
	{
		Command cmd = null;

		synchronized (commandsReceivedCnd)
		{
			if (commandsReceivedQueue.size() == 0 || ctrl.getListener() == null)
			{
				Log.d("stereoCamera","received: processor waiting");
				try {commandsReceivedCnd.wait();} catch(InterruptedException e){ return; }
			}

			if (commandsReceivedQueue.size() > 0)
			{
				cmd = commandsReceivedQueue.remove(0);
			}
		}

		if (!ctrl.getCommandReceiverIsRunning() || cmd == null)
			return;

		Log.d("stereoCamera","received: handling command " + cmd.cmdtype.name());

		ctrl.onCommand(cmd);
	}

	public void notifyListenerCreated()
	{
		synchronized(commandsReceivedCnd)
		{
			if (ctrl.getListener() != null)
			{
				Log.d("stereoCamera","command listener set");
				commandsReceivedCnd.notify();
			}
			else
			{
				Log.d("stereoCamera","command listener unset");
			}
		}
	}
}

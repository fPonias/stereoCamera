package com.munger.stereocamera.ip.command;

import android.util.Log;

import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketCtrlCtrl;
import com.munger.stereocamera.ip.utility.RemoteState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class CommCtrl
{
	private SocketCtrlCtrl socketCtrlCtrl;
	private Receiver receiver;
	private Sender sender;
	private Comm comm;
	private RemoteState remoteState;

	public CommCtrl(SocketCtrlCtrl socketCtrlCtrl) throws IOException
	{
		this.socketCtrlCtrl = socketCtrlCtrl;

		if (isMaster())
			comm = new Comm(socketCtrlCtrl.getSlave());
		else
			comm = new Comm(socketCtrlCtrl.getMaster());

		receiver = new Receiver(this);
		sender = new Sender(this);

		receiver.startCommandReceiver();
		sender.startCommandSender();
		startCommandTimeoutThread();

		remoteState = new RemoteState(this);
	}

	public RemoteState getRemoteState()
	{
		return remoteState;
	}

	public SocketCtrlCtrl getSocketCtrlCtrl()
	{
		return socketCtrlCtrl;
	}

	public Comm getComm()
	{
		return comm;
	}

	public interface StateListener
	{
		void onCommand(Command command);
		void onDisconnect();
	}

	public static abstract class ResponseListener
	{
		public long timeout = 0;

		public void onReceivingStarted(Command command) {}
		public void onReceiving(Command command, Command origCommand, float progress) {}
		public void onReceived(Command command, Command origCommand) {}
		public void onReceiveFailed(Command command) {}
		public void onSendingStarted(Command command) {}
		public void onSending(Command command, float progress) {}
		public void onSent(Command command) {}
		public void onSendFailed(Command command) {}
	}

	public interface IDefaultResponseListener
	{
		public void r(boolean success, Command command, Command originalCmd);
	}

	public static class DefaultResponseListener extends ResponseListener
	{
		private IDefaultResponseListener listener;

		public DefaultResponseListener(IDefaultResponseListener listener)
		{
			this.listener = listener;
		}

		public void onReceived(Command command, Command origCommand)
		{
			if (listener != null)
				listener.r(true, command, origCommand);
		}

		public void onReceiveFailed(Command command)
		{
			if (listener != null)
				listener.r(false, null, command);
		}
	}

	public static class ResponseListenerStr
	{
		public String index;
		public Command command;
		public ResponseListener listener;
		public long start;
		public long timeout;
	}

	private StateListener listener;
	public StateListener getListener()
	{
		return listener;
	}

	public void setListener(StateListener value)
	{
		listener = value;
		receiver.notifyListenerCreated();
	}

	public void disconnect()
	{
		setCommandReceiverIsRunning(false);
	}

	public String getCommandIndex(Command command)
	{
		return command.id + "-" + command.cmdtype.ordinal();
	}

	private boolean commandReceiverIsRunning = false;
	public boolean getCommandReceiverIsRunning()
	{
		return commandReceiverIsRunning;
	}

	public void setCommandReceiverIsRunning(boolean value)
	{
		commandReceiverIsRunning = value;

		if (value == true)
			return;

		receiver.stopAll();
		sender.stopAll();

		synchronized (commandResponseCondition)
		{
			commandResponseCondition.notify();
		}
	}

	private final Object commandResponseCondition = new Object();
	private HashMap<String, ResponseListenerStr> commandResponseListeners = new HashMap<>();

	public ResponseListenerStr getResponseListener(Command command)
	{
		String idx = getCommandIndex(command);

		synchronized (commandResponseCondition)
		{
			if (commandResponseListeners.containsKey(idx))
				return commandResponseListeners.get(idx);
			else
				return null;
		}
	}

	public ResponseListenerStr getAndRemoveResponseListener(Command command)
	{
		String idx = getCommandIndex(command);

		synchronized (commandResponseCondition)
		{
			if (commandResponseListeners.containsKey(idx))
			{
				commandResponseListeners.remove(idx);
				return commandResponseListeners.get(idx);
			}
			else
				return null;
		}
	}

	public void addResponseListener(ResponseListenerStr str)
	{
		if (str.listener == null)
			return;

		String idx = getCommandIndex(str.command);
		str.index = idx;
		str.start = 0;

		synchronized (commandResponseCondition)
		{
			commandResponseListeners.put(idx, str);
			commandResponseCondition.notify();
		}
	}

	public void updateResponseListener(Command command)
	{
		synchronized (commandResponseCondition)
		{
			String idx = getCommandIndex(command);

			if (commandResponseListeners.containsKey(idx))
			{
				ResponseListenerStr str = commandResponseListeners.get(idx);
				str.start = System.currentTimeMillis();
				commandResponseCondition.notify();
			}
		}
	}

	private void startCommandTimeoutThread()
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			Thread.currentThread().setName("Command Timeout Checker");
			while (commandReceiverIsRunning)
			{
				checkTimeouts();
			}
		}});
		t.start();
	}

	private void checkTimeouts()
	{
		long smallestWait = -1;
		ArrayList<ResponseListenerStr> events = new ArrayList<>();

		synchronized (commandResponseCondition)
		{
			boolean doWait = true;
			Set<String> keys = commandResponseListeners.keySet();
			for (String key : keys)
			{
				ResponseListenerStr listener = commandResponseListeners.get(key);

				if (listener.start > 0)
				{
					doWait = false;
					break;
				}
			}

			if (doWait)
			{
				try {commandResponseCondition.wait();} catch(InterruptedException e) {return;}

				if (!commandReceiverIsRunning)
					return;
			}

			Log.d("stereoCamera", "checking timeouts");


			long now = System.currentTimeMillis();
			ArrayList<String> toRemove = new ArrayList<>();
			for (String key : keys)
			{
				ResponseListenerStr listener = commandResponseListeners.get(key);

				if (listener.start > 0)
				{
					long then = listener.start + listener.timeout;
					long diff = then - now;

					if (diff <= 0)
					{
						events.add(listener);
						toRemove.add(key);
					}
					else
					{
						if (smallestWait < 0 || diff < smallestWait)
							{ smallestWait = diff; }
					}
				}
			}

			//avoid concurrentModificationException
			for (String key : toRemove)
			{
				commandResponseListeners.remove(key);
			}
		}

		if (!commandReceiverIsRunning)
			return;

		for (ResponseListenerStr listener : events)
		{
			Log.d("stereoCamera","command " + listener.command.cmdtype.name() + " timed out");
			listener.listener.onReceiveFailed(listener.command);
		}

		if (smallestWait > 0)
		{
			Log.d("stereoCamera","waiting " + smallestWait + " for next possible timeout");

			synchronized(commandResponseCondition)
			{
				try {commandResponseCondition.wait(smallestWait); } catch(InterruptedException e) { return; }

				if (!commandReceiverIsRunning)
					return;
			}
		}
	}

	public boolean isMasterConnected()
	{
		SocketCtrl master = socketCtrlCtrl.getSlave();

		if (master == null)
			return false;

		return master.isConnected();
	}

	public boolean isSlaveConnected()
	{
		SocketCtrl slave = socketCtrlCtrl.getMaster();

		if (slave == null)
			return false;

		return slave.isConnected();
	}

	public void cleanUpConnections()
	{
		SocketCtrl master = socketCtrlCtrl.getSlave();
		if (master != null && isMasterConnected())
			master.cleanUp();

		SocketCtrl slave = socketCtrlCtrl.getMaster();
		if (slave != null && isSlaveConnected())
			slave.cleanUp();
	}

	public boolean isMaster()
	{
		SocketCtrl master = socketCtrlCtrl.getSlave();

		if (master != null)
			return true;

		return false;
	}

	public void sendCommand(Command command, CommCtrl.IDefaultResponseListener listenerFunc)
	{
		CommCtrl.DefaultResponseListener listener = null;
		if (listenerFunc != null)
			listener = new DefaultResponseListener(listenerFunc);

		sendCommand(command, listener, 5000);
	}

	public void sendCommand(Command command, CommCtrl.ResponseListener listener, long timeout)
	{
		sender.sendCommand(command, listener, timeout);
	}

	public void onCommand(Command command)
	{
		if (listener != null)
			listener.onCommand(command);

		if (commandListeners.containsKey(command.cmdtype))
		{
			ArrayList<Listener> listenerList = commandListeners.get(command.cmdtype);

			for (Listener responder : listenerList)
			{
				responder.onCommand(command);
			}
		}

		if (command.expectsResponse && !command.isResponse)
		{
			command.isResponse = true;
			sendCommand(command, null);
		}
	}

	public interface Listener
	{
		void onCommand(Command command);
	}

	private HashMap<Command.Type, ArrayList<Listener>> commandListeners = new HashMap<>();
	public void registerListener(Command.Type type, Listener listener)
	{
		ArrayList<Listener> listenerList;
		if (commandListeners.containsKey(type))
			listenerList = commandListeners.get(type);
		else
		{
			listenerList = new ArrayList<>();
			commandListeners.put(type, listenerList);
		}

		listenerList.add(listener);
	}
}

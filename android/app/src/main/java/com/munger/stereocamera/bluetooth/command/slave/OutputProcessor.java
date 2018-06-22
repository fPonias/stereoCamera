package com.munger.stereocamera.bluetooth.command.slave;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by hallmarklabs on 3/6/18.
 */

public class OutputProcessor
{
	private boolean isCancelled = false;
	private boolean isProcessing = false;
	private final Object replyLock = new Object();
	private LinkedList<SlaveCommand> replies = new LinkedList<>();

	public OutputProcessor()
	{
	}

	public void cleanUp()
	{
		synchronized (replyLock)
		{
			if (isCancelled)
				return;

			isCancelled = true;
			replyLock.notify();
		}
	}

	public void start()
	{
		Thread t = new Thread(new Runnable() { public void run()
		{
			while(!isCancelled)
			{
				try
				{
					replyProcessor();
				}
				catch(IOException e){
					return;
				}
			}
		}});
		t.start();
	}

	private void replyProcessor() throws IOException
	{
		SlaveCommand command;
		synchronized (replyLock)
		{
			if (replies.isEmpty())
			{
				try {replyLock.wait();}
				catch(InterruptedException e){return;}
			}

			if (isCancelled)
				return;

			if (isProcessing)
				return;

			command = replies.removeFirst();
			isProcessing = true;
		}

		command.doSend();

		synchronized (replyLock)
		{
			isProcessing = false;
		}
	}

	public void sendCommand(SlaveCommand command)
	{
		synchronized (replyLock)
		{
			replies.add(command);

			replyLock.notify();
		}
	}
}

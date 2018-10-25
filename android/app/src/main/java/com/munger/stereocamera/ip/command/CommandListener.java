package com.munger.stereocamera.ip.command;

public interface CommandListener
{
	void onCommand(Command command);
	void onDisconnect();
}

package com.munger.stereocamera.ip;

public class IPListeners
{
	public interface SetupListener
	{
		void onSetup();
	}

	public interface ConnectListener
	{
		void onFailed();
		void onDiscoverable();
		void onConnected();
		void onDisconnected();
	}
}

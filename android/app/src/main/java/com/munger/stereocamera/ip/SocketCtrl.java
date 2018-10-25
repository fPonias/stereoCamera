package com.munger.stereocamera.ip;

import com.munger.stereocamera.ip.utility.RemoteState;

public interface SocketCtrl
{
	Socket getSocket();
	boolean isConnected();
	void cleanUp();
}

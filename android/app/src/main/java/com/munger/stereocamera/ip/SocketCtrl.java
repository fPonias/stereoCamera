package com.munger.stereocamera.ip;

import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.slave.SlaveComm;
import com.munger.stereocamera.ip.utility.RemoteState;

public interface SocketCtrl
{
	Socket getSocket();
	boolean isConnected();
	void cleanUp();
	RemoteState getRemoteState();
}

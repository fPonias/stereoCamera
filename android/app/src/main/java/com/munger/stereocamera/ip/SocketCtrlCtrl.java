package com.munger.stereocamera.ip;

import com.munger.stereocamera.ip.command.master.MasterComm;
import com.munger.stereocamera.ip.command.slave.SlaveComm;

public interface SocketCtrlCtrl
{
	boolean getIsSetup();
	SocketCtrl getMaster();
	SocketCtrl getSlave();
	MasterComm getMasterComm();
	SlaveComm getSlaveComm();
}

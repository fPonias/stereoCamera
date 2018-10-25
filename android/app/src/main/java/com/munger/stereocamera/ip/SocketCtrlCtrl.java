package com.munger.stereocamera.ip;

import com.munger.stereocamera.ip.command.CommCtrl;

public interface SocketCtrlCtrl
{
	boolean getIsSetup();
	SocketCtrl getSlave();
	SocketCtrl getMaster();
	Boolean isMaster();
}

package com.munger.stereocamera.ip.ethernet;

import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.SocketCtrl;
import com.munger.stereocamera.ip.SocketCtrlCtrl;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EthernetCtrl implements SocketCtrlCtrl
{
	public String bytesToHex(byte[] bytes)
	{
		StringBuilder sbuf = new StringBuilder();
		for(int idx=0; idx < bytes.length; idx++)
		{
			int intVal = bytes[idx] & 0xff;
			if (intVal < 0x10) sbuf.append("0");
			sbuf.append(Integer.toHexString(intVal).toUpperCase());
		}

		return sbuf.toString();
	}

	public ArrayList<String> getIPAddress(boolean useIPv4)
	{
		ArrayList<String> ret = new ArrayList<>();

		try
		{
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

			for (NetworkInterface intf : interfaces)
			{
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());

				for (InetAddress addr : addrs)
				{
					if (!addr.isLoopbackAddress())
					{
						String sAddr = addr.getHostAddress();
						//boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						boolean isIPv4 = sAddr.indexOf(':')<0;

						if (useIPv4)
						{
							if (isIPv4)
								ret.add(sAddr);
						}
						else
						{
							if (!isIPv4)
							{
								int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
								String entry = delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
								ret.add(entry);
							}
						}
					}
				}
			}
		}
		catch (Exception ignored)
		{ } // for now eat exceptions

		return ret;
	}

	private boolean isSetup = false;

	public boolean getIsSetup()
	{
		return isSetup;
	}

	@Override
	public SocketCtrl getSlave()
	{
		return slave;
	}

	@Override
	public SocketCtrl getMaster()
	{
		return master;
	}

	private ArrayList<String> addresses = new ArrayList<>();

	public ArrayList<String> getAddresses()
	{
		return addresses;
	}

	public void setup(IPListeners.SetupListener listener)
	{
		addresses = getIPAddress(true);

		isSetup = true;
		listener.onSetup();
	}

	private String ipAddress;
	public static final int PORT = 36111;
	private EthernetSlave slave;
	private EthernetMaster master;
	private Object lock = new Object();

	public String getIpAddress()
	{
		return ipAddress;
	}

	public void connect(String ipAddress, IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (slave == null)
				slave = new EthernetSlave();
		}

		this.ipAddress = ipAddress;
		slave.connect(ipAddress, PORT, listener);
	}

	public void cancelConnect()
	{
		synchronized (lock)
		{
			if (slave != null)
				slave.cleanUp();

			this.ipAddress = null;
			slave = null;
		}
	}

	public void listen(IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (master == null)
				master = new EthernetMaster(listener);
			else
				master.setConnectListener(listener);

			this.ipAddress = null;
		}

		master.listen(PORT);
	}

	public void cancelListen()
	{
		synchronized (lock)
		{
			if (master != null)
				master.cleanUp();

			master = null;
		}
	}

	public Boolean isMaster()
	{
		if (!isSetup)
			return null;

		if (master != null)
			return true;
		else
			return false;
	}
}

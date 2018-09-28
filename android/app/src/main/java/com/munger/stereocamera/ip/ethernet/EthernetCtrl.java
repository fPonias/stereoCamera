package com.munger.stereocamera.ip.ethernet;

import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.bluetooth.BluetoothMaster;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EthernetCtrl
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

	public static final int PORT = 36111;
	private EthernetMaster master;
	private EthernetSlave slave;
	private Object lock = new Object();

	public void connect(String ipAddress, IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (master == null)
				master = new EthernetMaster();
		}

		master.connect(ipAddress, PORT, listener);
	}

	public void cancelConnect()
	{
		synchronized (lock)
		{
			if (master != null)
				master.cleanUp();

			master = null;
		}
	}

	public void listen(IPListeners.ConnectListener listener)
	{
		synchronized (lock)
		{
			if (slave == null)
				slave = new EthernetSlave();
		}

		slave.listen(PORT);
	}

	public void cancelListen()
	{
		synchronized (lock)
		{
			if (slave != null)
				slave.cleanUp();

			slave = null;
		}
	}
}

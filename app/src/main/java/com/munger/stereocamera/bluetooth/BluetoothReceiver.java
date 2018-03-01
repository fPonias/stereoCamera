package com.munger.stereocamera.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;

import java.util.Set;

/**
 * Created by hallmarklabs on 2/25/18.
 */

public class BluetoothReceiver extends BroadcastReceiver
{

	@Override
	public void onReceive(Context context, Intent intent)
	{
		MyApplication instance = MyApplication.getInstance();

		if (instance == null)
			return;

		String action = intent.getAction();
		Bundle b = intent.getExtras();
		BaseActivity act = instance.getCurrentActivity();


		if(!(act instanceof MainActivity))
			return;

		MainActivity mact = (MainActivity) act;

		String device;

		if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
		{
			BluetoothDevice dev = (BluetoothDevice) b.get(BluetoothDevice.EXTRA_DEVICE);
			mact.handleNewConnection(dev);
		}
		else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
		{
			BluetoothDevice dev = (BluetoothDevice) b.get(BluetoothDevice.EXTRA_DEVICE);
			mact.handleDisconnection(dev);
		}
	}
}

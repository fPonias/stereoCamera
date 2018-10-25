package com.munger.stereocamera.fragment;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.ip.ethernet.EthernetCtrl;
import com.munger.stereocamera.ip.ethernet.EthernetSlave;
import com.munger.stereocamera.utility.Preferences;

import java.io.IOException;
import java.util.ArrayList;

public class ConnectEthernetSubFragment
{
	private ViewGroup view;
	private ConnectFragment parent;

	private ViewGroup ipAddressList;
	private EditText ipAddressTarget;
	private Button ipListenButton;
	private Button ipConnectButton;

	private EthernetCtrl ethCtrl;
	private AlertDialog listenDialog = null;
	private AlertDialog againDialog;

	public ConnectEthernetSubFragment(ConnectFragment parent, ViewGroup target)
	{
		view = target;
		this.parent = parent;

		ipAddressList =  view.findViewById(R.id.ipAddressList);
		ipAddressTarget = view.findViewById(R.id.ipAddressTarget);
		ipConnectButton = view.findViewById(R.id.ipConnectButton);
		ipListenButton = view.findViewById(R.id.ipListenButton);

		MainActivity.getInstance().setupEthernetServer(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				ArrayList<String> addresses = MainActivity.getInstance().getEthCtrl().getAddresses();
				for(String address : addresses)
				{
					TextView tv = new TextView(MainActivity.getInstance());
					tv.setText(address);
					ipAddressList.addView(tv);
				}

				if (addresses.size() > 0)
				{
					String address = addresses.get(0);
					String[] parts = address.split("\\.");
					parts[3] = "1";
					address = "";
					for (int i = 0; i < parts.length; i++)
					{
						if (i != 0)
							address += ".";

						address += parts[i];
					}

					ipAddressTarget.setText(address);
				}
			}
		});

		ipConnectButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				connectIPClicked();
			}
		});

		ipListenButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				listenIPClicked();
			}
		});
	}

	public void cleanUp()
	{
		if (ethCtrl != null)
		{
			ethCtrl.cancelConnect();
			ethCtrl.cancelListen();
		}
	}

	private void listenIPClicked()
	{
		ethCtrl = MainActivity.getInstance().getEthCtrl();
		ethCtrl.setup(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				ethCtrl = MainActivity.getInstance().getEthCtrl();

				showEthListenDialog();
				ethCtrl.listen(ethListener);
			}
		});
	}

	private void showEthListenDialog()
	{
		if (listenDialog != null)
			return;

		AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
		listenDialog = builder
				.setTitle("Listening")
				.setMessage(parent.getString(R.string.bluetooth_listen_message))
				.setCancelable(true)
				.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int i)
				{
					ethCtrl.cancelListen();
					listenDialog = null;
				}})
				.create();
		listenDialog.setCanceledOnTouchOutside(false);
		listenDialog.show();
	}


	private void connectIPClicked()
	{
		ethCtrl = MainActivity.getInstance().getEthCtrl();
		ethCtrl.setup(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				String addrStr =  ipAddressTarget.getText().toString().trim();
				boolean isValid = EthernetSlave.checkIP(addrStr);

				if (!isValid)
					return;

				ethCtrl = MainActivity.getInstance().getEthCtrl();


				parent.handler.post(new Runnable() {public void run()
				{
					showEthListenDialog();
				}});

				ethCtrl.connect(addrStr, ethListener);
			}
		});
	}

	private IPListeners.ConnectListener ethListener = new IPListeners.ConnectListener()
	{
		@Override
		public void onConnected()
		{
			try
			{
				CommCtrl ctrl = new CommCtrl(ethCtrl);
				MainActivity.getInstance().setCtrl(ctrl);
			}
			catch(IOException e){
				return;
			}

			parent.handler.post(new Runnable() {public void run()
			{
				if (listenDialog == null)
					return;

				//listenDialog.setStatus("Connected");
				listenDialog.dismiss();
				listenDialog = null;

				if (ethCtrl.isMaster())
				{
					parent.prefs.setRole(Preferences.Roles.MASTER);
					parent.prefs.setClient(ethCtrl.getIpAddress());

					MainActivity.getInstance().startMasterView();
				}
				else
				{
					parent.prefs.setRole(Preferences.Roles.SLAVE);
					parent.prefs.setClient(null);

					MainActivity.getInstance().startSlaveView();
				}
			}});
		}

		@Override
		public void onDiscoverable()
		{
		}

		@Override
		public void onFailed()
		{
			parent.handler.post(new Runnable() {public void run()
			{
				if (listenDialog == null || againDialog != null)
					return;

				listenDialog.dismiss();
				listenDialog = null;

				againDialog = new AlertDialog.Builder(parent.getActivity())
						.setTitle(R.string.bluetooth_listen_failed_error)
						.setMessage(R.string.bluetooth_try_again_message)
						.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							againDialog.dismiss();
							againDialog = null;
						}})
						.setPositiveButton(R.string.retry_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							againDialog.dismiss();
							againDialog = null;

							listenIPClicked();
						}})
						.create();
				againDialog.setCanceledOnTouchOutside(false);
				againDialog.show();
			}});
		}

		@Override
		public void onDisconnected()
		{

		}
	};
}

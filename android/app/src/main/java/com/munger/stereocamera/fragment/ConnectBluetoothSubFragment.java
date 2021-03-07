package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import android.nfc.tech.Ndef;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothDiscoverer;
import com.munger.stereocamera.ip.bluetooth.BluetoothMaster;
import com.munger.stereocamera.ip.bluetooth.BluetoothSlave;
import com.munger.stereocamera.ip.command.CommCtrl;
import com.munger.stereocamera.utility.data.Client;
import com.munger.stereocamera.utility.data.ClientViewModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class ConnectBluetoothSubFragment
{
	private ViewGroup view;
	private ConnectFragment parent;

	private Button connectButton;
	private Button listenDiscoverButton;

	private BluetoothCtrl btCtrl;

	public ConnectBluetoothSubFragment(ConnectFragment parent, ViewGroup target)
	{
		this.parent = parent;
		view = target;


		connectButton = view.findViewById(R.id.connectButton);
		listenDiscoverButton = view.findViewById(R.id.listenDiscoverButton);

		connectButton.setOnClickListener(view -> discoverClicked());
		listenDiscoverButton.setOnClickListener(view -> listenDiscoverClicked());
	}

	private DiscoverDialog discoverDialog;
	private HashMap<String, BluetoothDevice> devices;
	private BluetoothDiscoverer discoverer;

	private static int CONNECT_RETRIES = 2;
	private static long CONNECT_RETRY_WAIT = 1500;

	public void connect(Client client)
	{
		MyApplication.getInstance().setupBTServer(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				discoverer = btCtrl.getDiscoverer();
				deviceSelected(client.address, CONNECT_RETRIES);
			}
		});
	}

	private void discoverClicked()
	{
		MyApplication.getInstance().setupBTServer(() -> {
			btCtrl = MyApplication.getInstance().getBtCtrl();
			connectClicked2();
		});

	}

	private void connectClicked2()
	{
		try
		{
			discoverDialog = new DiscoverDialog();
			devices = new HashMap<>();
			discoverer = btCtrl.getDiscoverer();

			btCtrl.discover(new BluetoothDiscoverer.DiscoverListener()
			{
				public void onDiscovered(BluetoothDevice device)
				{
					String id = device.getAddress();
					String name = device.getName();

					devices.put(id, device);

					discoverDialog.addDiscovery(id, name);
				}

				@Override
				public void onKnown(BluetoothDevice device)
				{
					String id = device.getAddress();
					String name = device.getName();

					devices.put(id, device);

					discoverDialog.addKnown(id, name);
				}
			});
		}
		catch(BluetoothCtrl.BluetoothDiscoveryFailedException e)
		{
			Toast.makeText(parent.getContext(), R.string.bluetooth_discovery_failed_error, Toast.LENGTH_LONG).show();
			return;
		}

		discoverDialog.show(parent.getActivity().getSupportFragmentManager(), "discoverDialog");
		discoverDialog.setListener(new DiscoverDialog.ActionListener()
		{
			@Override
			public void cancelled()
			{
				discoverer.cancelDiscover();
			}

			@Override
			public void selected(String id)
			{
				deviceSelected(id, 0);
			}
		});
	}

	private BluetoothMaster master;

	private void deviceSelected(String id, int retries)
	{
		if (discoverer != null)
			discoverer.cancelDiscover();


		BluetoothDevice device;
		if (devices != null && devices.containsKey(id))
			device = devices.get(id);
		else
			device = btCtrl.getKnownDevice(id);

		if (device == null)
			return;

		deviceSelected(device, retries);
	}

	private AlertDialog connectDialog;

	public void deviceSelected(final BluetoothDevice device, final int retries)
	{
		if (btCtrl == null)
			btCtrl = MyApplication.getInstance().getBtCtrl();

		parent.handler.post(new Runnable() {public void run()
		{
			if (discoverDialog != null)
				discoverDialog.dismiss();

			if (device == null)
				return;

			String message = parent.getResources().getString(R.string.bluetooth_connecting_message) + device.getName();
			connectDialog = new AlertDialog.Builder(parent.getContext())
					.setMessage(message)
					.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
					{

						if (connectDialog != null)
							connectDialog.dismiss();

						connectDialog = null;
						btCtrl.cancelConnect();
					}})
					.create();
			connectDialog.setCanceledOnTouchOutside(false);
			connectDialog.show();
		}});

		btCtrl.connect(device, new ConnectListener(device, retries));
	}

	private class SilentConnectListener implements IPListeners.ConnectListener
	{
		BluetoothDevice device;
		int retries;

		public SilentConnectListener(BluetoothDevice device, int retries)
		{
			this.device = device;
			this.retries = retries;
		}

		@Override
		public void onFailed()
		{
			if (retries > 0)
			{
				try{Thread.sleep(CONNECT_RETRY_WAIT);} catch(InterruptedException e){return;}
				retries--;
				btCtrl.connect(device, this);
				return;
			}
		}

		@Override
		public void onDiscoverable()
		{}

		@Override
		public void onConnected()
		{
			try
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				CommCtrl ctrl = new CommCtrl(btCtrl);
				MyApplication.getInstance().setCtrl(ctrl);
			}
			catch(IOException e){return;}

			ClientViewModel clientModel = parent.getClientModel();
			Client cli = clientModel.get(device.getAddress());
			cli.lastUsed = System.currentTimeMillis();
			cli.role = Client.Role.MASTER;
			clientModel.update(cli);

			clientModel.setCurrentClient(cli);

			parent.handler.post(() ->
			{
				MainActivity act = MainActivity.getInstance();
				if (act != null)
					act.startMasterView();
			});
		}

		@Override
		public void onDisconnected()
		{}
	};

	private class ConnectListener extends SilentConnectListener
	{
		public ConnectListener(BluetoothDevice device, int retries)
		{
			super(device, retries);
		}

		@Override
		public void onFailed()
		{

			if (retries == 0)
			{
				parent.handler.post(new Runnable() {public void run()
				{
					if (connectDialog != null)
					{
						connectDialog.dismiss();
						Toast.makeText(parent.getContext(), R.string.bluetooth_connect_failed_error, Toast.LENGTH_LONG).show();
					}
				}});
			}

			super.onFailed();
		}

		@Override
		public void onConnected()
		{
			super.onConnected();

			parent.handler.post(() ->
			{
				Toast.makeText(parent.getContext(), R.string.bluetooth_connect_success, Toast.LENGTH_LONG).show();

				if (connectDialog != null)
					connectDialog.dismiss();
			});
		}
	}


	private AlertDialog listenDialog = null;
	private BluetoothSlave listenSlave = null;

	private void listenDiscoverClicked()
	{
		MyApplication.getInstance().setupBTServer(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				listenAndEnableDiscovery();
			}
		});
	}

	private AlertDialog againDialog;
	private boolean discovering = false;

	private IPListeners.ConnectListener listenListener = new IPListeners.ConnectListener()
	{
		@Override
		public void onConnected()
		{
			try
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				CommCtrl ctrl = new CommCtrl(btCtrl);
				MyApplication.getInstance().setCtrl(ctrl);
			}
			catch(IOException e){return;}

			ClientViewModel clientModel = parent.getClientModel();
			String address = btCtrl.getMaster().getLastConnected().getAddress();
			Client cli = clientModel.get(address);
			cli.lastUsed = System.currentTimeMillis();
			cli.role = Client.Role.SLAVE;
			clientModel.update(cli);

			clientModel.setCurrentClient(cli);

			parent.handler.post(() ->
			{
				if (listenDialog != null)
					listenDialog.dismiss();

				listenDialog = null;

				MainActivity.getInstance().startSlaveView();
			});
		}

		@Override
		public void onDiscoverable()
		{
			parent.handler.post(new Runnable() {public void run()
			{
				if (listenDialog == null)
					return;

				//listenDialog.setStatus("Discoverable");
			}});
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

				againDialog = new AlertDialog.Builder(parent.getContext())
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

							if (discovering)
								listenAndEnableDiscovery();
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

	private void listenAndEnableDiscovery()
	{
		discovering = true;
		btCtrl.listen(true, listenListener);

		showListenDialog();
	}

	private void showListenDialog()
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
					btCtrl.cancelListen();
					listenDialog = null;
				}})
				.create();
		listenDialog.setCanceledOnTouchOutside(false);
		listenDialog.show();
	}

	public void cleanUp()
	{
		discovering = false;

		if (btCtrl != null)
			btCtrl.cleanUp();
	}

	public void reconnect()
	{
		MyApplication.getInstance().setupBTServer(() ->
		{
			btCtrl = MyApplication.getInstance().getBtCtrl();
			Set<BluetoothDevice> devices = btCtrl.getKnownDevices();
			btCtrl.listen(false, listenListener);

			/*for (BluetoothDevice device : devices)
			{
				String addr = device.getAddress();
				if (!parent.getClientModel().has(addr))
					continue;

				Client client = parent.getClientModel().get(addr);
				if (client.role == Client.Role.MASTER)
					btCtrl.connect(device, new SilentConnectListener(device, CONNECT_RETRIES));
			}*/
		});
	}
}

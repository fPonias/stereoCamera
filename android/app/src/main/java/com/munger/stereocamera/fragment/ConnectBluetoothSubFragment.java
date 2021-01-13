package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
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
import com.munger.stereocamera.utility.data.ClientViewModelProvider;

import java.io.IOException;
import java.util.HashMap;

public class ConnectBluetoothSubFragment
{
	private ViewGroup view;
	private ConnectFragment parent;

	private TextView restoreTitle;
	private TextView discoverTitle;
	private Button connectButton;
	private Button listenButton;
	private Button discoverButton;
	private Button listenDiscoverButton;

	private BluetoothCtrl btCtrl;

	public ConnectBluetoothSubFragment(ConnectFragment parent, ViewGroup target)
	{
		this.parent = parent;
		view = target;

		discoverTitle = view.findViewById(R.id.discoverTitle);
		restoreTitle = view.findViewById(R.id.restoreTitle);

		connectButton = view.findViewById(R.id.connectButton);
		listenButton = view.findViewById(R.id.listenButton);
		discoverButton = view.findViewById(R.id.discoverButton);
		listenDiscoverButton = view.findViewById(R.id.listenDiscoverButton);



		Client last = parent.getClientModel().getLast();
		if (last != null && last.role == Client.Role.MASTER)
		{
			restoreTitle.setVisibility(View.VISIBLE);
			listenButton.setVisibility(View.GONE);
			connectButton.setVisibility(View.VISIBLE);
		}
		else if (last != null && last.role == Client.Role.SLAVE)
		{
			restoreTitle.setVisibility(View.VISIBLE);
			listenButton.setVisibility(View.VISIBLE);
			connectButton.setVisibility(View.GONE);
		}
		else
		{
			restoreTitle.setVisibility(View.GONE);
			listenButton.setVisibility(View.GONE);
			connectButton.setVisibility(View.GONE);
		}

		connectButton.setOnClickListener(new View.OnClickListener() {public void onClick(View view)
		{
			connect(last);
		}});

		listenButton.setOnClickListener(new View.OnClickListener() {public void onClick(View view)
		{
			listen();
		}});

		discoverButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				discoverClicked();
			}
		});

		listenDiscoverButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				listenDiscoverClicked();
			}
		});
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
		MyApplication.getInstance().setupBTServer(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				connectClicked2();
			}
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

		deviceSelected2(device, retries);
	}

	private void deviceSelected2(final BluetoothDevice device, final int retries)
	{
		btCtrl.connect(device, new IPListeners.ConnectListener()
		{
			@Override
			public void onFailed()
			{
				if (retries > 0)
				{
					try{Thread.sleep(CONNECT_RETRY_WAIT);} catch(InterruptedException e){return;}
					deviceSelected2(device, retries - 1);
					return;
				}

				parent.handler.post(new Runnable() {public void run()
				{
					if (connectDialog != null)
					{
						connectDialog.dismiss();
						Toast.makeText(parent.getContext(), R.string.bluetooth_connect_failed_error, Toast.LENGTH_LONG).show();
					}
				}});
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
					Toast.makeText(parent.getContext(), R.string.bluetooth_connect_success, Toast.LENGTH_LONG).show();

					if (connectDialog != null)
						connectDialog.dismiss();

					MainActivity act = MainActivity.getInstance();
					if (act != null)
						act.startMasterView();
				});
			}

			@Override
			public void onDisconnected()
			{}
		});
	}


	private AlertDialog listenDialog = null;
	private BluetoothSlave listenSlave = null;

	public void listen()
	{
		MyApplication.getInstance().setupBTServer(new IPListeners.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				listenForMaster();
			}
		});
	}

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
				if (listenDialog == null)
					return;

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
							else
								listenForMaster();
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

	public void listenForMaster()
	{
		MyApplication.getInstance().setupBTServer(new IPListeners.SetupListener() { public void onSetup()
		{
			btCtrl = MyApplication.getInstance().getBtCtrl();

			showListenDialog();

			discovering = false;
			btCtrl.listen(false, listenListener);
		}});
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
}

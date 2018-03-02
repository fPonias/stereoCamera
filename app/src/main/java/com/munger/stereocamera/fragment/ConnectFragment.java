package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.R;
import com.munger.stereocamera.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.bluetooth.BluetoothDiscoverer;
import com.munger.stereocamera.bluetooth.BluetoothMaster;
import com.munger.stereocamera.bluetooth.BluetoothSlave;

import java.util.HashMap;

/**
 * Created by hallmarklabs on 2/22/18.
 */

public class ConnectFragment extends Fragment
{
	private Handler handler;

	private TextView statusView;
	private Button connectButton;
	private Button listenButton;
	private View view;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.fragment_connect, container, false);

		handler = new Handler(Looper.getMainLooper());

		findViews();
		setupViews();

		return view;
	}

	private boolean firstConnect = true;

	@Override
	public void onStart()
	{
		super.onStart();

		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				BluetoothCtrl.Roles role = btCtrl.getLastRole();
				String deviceName = btCtrl.getLastClient();

				if (role == BluetoothCtrl.Roles.MASTER)
				{
					if (btCtrl.isMasterConnected())
					{
						return;
					}

					BluetoothDevice device = btCtrl.getDiscoverer().getKnownDevice(deviceName);
					deviceSelected(device);
				}
				else if (role == BluetoothCtrl.Roles.SLAVE)
				{
					listenForMaster();
				}
			}
		});
	}

	private void findViews()
	{
		statusView = view.findViewById(R.id.statusText);
		connectButton = view.findViewById(R.id.connectButton);
		listenButton = view.findViewById(R.id.listenButton);
	}

	private void setupViews()
	{
		statusView.setText("Idle");

		connectButton.setOnClickListener(new View.OnClickListener() {public void onClick(View view)
		{
			connectClicked();
		}});

		listenButton.setOnClickListener(new View.OnClickListener() {public void onClick(View view)
		{
			listenClicked();
		}});
	}

	private BluetoothCtrl btCtrl;

	private DiscoverDialog discoverDialog;
	private HashMap<String, BluetoothDevice> devices;
	private BluetoothDiscoverer discoverer;

	private void connectClicked()
	{
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
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
			discoverer = MyApplication.getInstance().getBtCtrl().getDiscoverer();

			btCtrl.discover(new BluetoothDiscoverer.DiscoverListener()
			{
				public void onDiscovered(BluetoothDevice device)
				{
					String id = device.getAddress();
					String name = device.getName();

					devices.put(id, device);

					discoverDialog.addDiscovery(id, name);
				}
			});
		}
		catch(BluetoothCtrl.BluetoothDiscoveryFailedException e)
		{
			Toast.makeText(getActivity(), R.string.bluetooth_discovery_failed_error, Toast.LENGTH_LONG).show();
			return;
		}

		discoverDialog.show(getActivity().getSupportFragmentManager(), "discoverDialog");
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
				deviceSelected(id);
			}
		});
	}

	private BluetoothMaster master;

	private void deviceSelected(String id)
	{
		discoverer.cancelDiscover();

		BluetoothDevice device = devices.get(id);
		deviceSelected(device);
	}

	public void deviceSelected(final BluetoothDevice device)
	{
		if (btCtrl == null)
			btCtrl = MyApplication.getInstance().getBtCtrl();

		btCtrl.connect(device, new BluetoothCtrl.ConnectListener()
		{
			@Override
			public void onFailed()
			{
				handler.post(new Runnable() {public void run()
				{
					Toast.makeText(getActivity(), R.string.bluetooth_connect_failed_error, Toast.LENGTH_LONG).show();
				}});
			}

			@Override
			public void onConnected()
			{
				handler.post(new Runnable() {public void run()
				{
					Toast.makeText(getActivity(), R.string.bluetooth_connect_success, Toast.LENGTH_LONG).show();

					if (discoverDialog != null)
						discoverDialog.dismiss();

					btCtrl.setLastRole(BluetoothCtrl.Roles.MASTER);
					btCtrl.setLastClient(device.getName());

					BaseActivity act = MyApplication.getInstance().getCurrentActivity();
					if (act instanceof MainActivity)
						((MainActivity) act).startMasterView();
				}});
			}

			@Override
			public void onDisconnected()
			{

			}
		});
	}


	private ListenDialog listenDialog = null;
	private BluetoothSlave listenSlave = null;

	private void listenClicked()
	{
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				listenClicked2();
			}
		});
	}

	private AlertDialog againDialog;

	private BluetoothCtrl.ConnectListener listenListener = new BluetoothCtrl.ConnectListener()
	{
		@Override
		public void onConnected()
		{
			handler.post(new Runnable() {public void run()
			{
				listenDialog.setStatus("Connected");
				listenDialog.dismiss();

				btCtrl.setLastRole(BluetoothCtrl.Roles.SLAVE);
				btCtrl.setLastClient(null);

				BaseActivity act = MyApplication.getInstance().getCurrentActivity();
				if (act instanceof MainActivity)
					((MainActivity) act).startSlaveView();
			}});
		}

		@Override
		public void onFailed()
		{
			handler.post(new Runnable() {public void run()
			{
				listenDialog.dismiss();

				againDialog = new AlertDialog.Builder(getActivity())
						.setTitle(R.string.bluetooth_listen_failed_error)
						.setMessage(R.string.bluetooth_try_again_message)
						.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							againDialog.dismiss();
						}})
						.setPositiveButton(R.string.retry_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
						{
							againDialog.dismiss();
							listenForMaster();
						}})
						.create();
				againDialog.show();
			}});
		}

		@Override
		public void onDisconnected()
		{

		}
	};

	private void listenClicked2()
	{
		listenDialog = new ListenDialog();
		listenDialog.show(getActivity().getSupportFragmentManager(), "listenDialog");
		listenDialog.setStatus("Starting Listener");
		listenDialog.setListener(new ListenDialog.ActionListener()
		{
			@Override
			public void cancelled()
			{
				btCtrl.cancelListen();
			}
		});

		btCtrl.listen(true, listenListener);
	}

	public void listenForMaster()
	{
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
			btCtrl = MyApplication.getInstance().getBtCtrl();

			listenDialog = new ListenDialog();
			listenDialog.show(getActivity().getSupportFragmentManager(), "listenDialog");
			listenDialog.setStatus("Starting Listener");
			listenDialog.setListener(new ListenDialog.ActionListener()
			{
				@Override
				public void cancelled()
				{
					btCtrl.cancelListen();
				}
			});

			btCtrl.listen(false, listenListener);
			listenDialog.setStatus("Listening");
			}
		});
	}

	public void slaveConnected(final BluetoothDevice device)
	{
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
			btCtrl = MyApplication.getInstance().getBtCtrl();

			deviceSelected(device);
			}
		});
	}
}

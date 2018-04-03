package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
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
import com.munger.stereocamera.utility.AudioSync;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.widget.AudioSyncWidget;
import com.munger.stereocamera.widget.ThumbnailWidget;

import java.util.HashMap;

public class ConnectFragment extends Fragment
{
	private Handler handler;

	private TextView restoreTitle;
	private TextView discoverTitle;
	private Button connectButton;
	private Button listenButton;
	private Button discoverButton;
	private Button listenDiscoverButton;
	private ThumbnailWidget thumbnail;
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
	}

	@Override
	public void onResume()
	{
		super.onResume();
		thumbnail.update();
	}

	private void findViews()
	{
		discoverTitle = view.findViewById(R.id.discoverTitle);
		restoreTitle = view.findViewById(R.id.restoreTitle);

		connectButton = view.findViewById(R.id.connectButton);
		listenButton = view.findViewById(R.id.listenButton);
		discoverButton = view.findViewById(R.id.discoverButton);
		listenDiscoverButton = view.findViewById(R.id.listenDiscoverButton);

		thumbnail = view.findViewById(R.id.thumbnail);
	}

	private void setupViews()
	{
		prefs = MyApplication.getInstance().getPrefs();
		Preferences.Roles role = prefs.getRole();

		if (role == Preferences.Roles.MASTER)
		{
			restoreTitle.setVisibility(View.VISIBLE);
			listenButton.setVisibility(View.GONE);
			connectButton.setVisibility(View.VISIBLE);
		}
		else if (role == Preferences.Roles.SLAVE)
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
			connect();
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

		thumbnail.update();
		thumbnail.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			thumbnailClicked();
		}});
	}

	private BluetoothCtrl btCtrl;
	private Preferences prefs;

	private DiscoverDialog discoverDialog;
	private HashMap<String, BluetoothDevice> devices;
	private BluetoothDiscoverer discoverer;

	private static int CONNECT_RETRIES = 2;
	private static long CONNECT_RETRY_WAIT = 1500;

	public void connect()
	{
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
				btCtrl = MyApplication.getInstance().getBtCtrl();
				discoverer = MyApplication.getInstance().getBtCtrl().getDiscoverer();
				String id = prefs.getClient();

				if (id != null && id.length() > 0)
					deviceSelected(id, CONNECT_RETRIES);
			}
		});
	}

	private void discoverClicked()
	{
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
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

		handler.post(new Runnable() {public void run()
		{
			if (discoverDialog != null)
				discoverDialog.dismiss();

			if (device == null)
				return;

			String message = getResources().getString(R.string.bluetooth_connecting_message) + device.getName();
			connectDialog = new AlertDialog.Builder(getActivity())
					.setMessage(message)
					.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialogInterface, int i)
					{
						connectDialog.dismiss();
						connectDialog = null;
						btCtrl.cancelConnect();
					}})
					.create();
			connectDialog.show();
		}});

		deviceSelected2(device, retries);
	}

	private void deviceSelected2(final BluetoothDevice device, final int retries)
	{
		btCtrl.connect(device, new BluetoothCtrl.ConnectListener()
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

				handler.post(new Runnable() {public void run()
				{
					if (connectDialog != null)
						connectDialog.dismiss();

					Toast.makeText(getActivity(), R.string.bluetooth_connect_failed_error, Toast.LENGTH_LONG).show();
				}});
			}

			@Override
			public void onDiscoverable()
			{}

			@Override
			public void onConnected()
			{
				handler.post(new Runnable() {public void run()
				{
					Toast.makeText(getActivity(), R.string.bluetooth_connect_success, Toast.LENGTH_LONG).show();

					if (connectDialog != null)
						connectDialog.dismiss();

					prefs.setRole(Preferences.Roles.MASTER);
					prefs.setClient(device.getAddress());

					BaseActivity act = MyApplication.getInstance().getCurrentActivity();
					if (act instanceof MainActivity)
						((MainActivity) act).startMasterView();
				}});
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
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
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
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
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

	private BluetoothCtrl.ConnectListener listenListener = new BluetoothCtrl.ConnectListener()
	{
		@Override
		public void onConnected()
		{
			handler.post(new Runnable() {public void run()
			{
				if (listenDialog == null)
					return;

				//listenDialog.setStatus("Connected");
				listenDialog.dismiss();
				listenDialog = null;

				prefs.setRole(Preferences.Roles.SLAVE);
				prefs.setClient(null);

				BaseActivity act = MyApplication.getInstance().getCurrentActivity();
				if (act instanceof MainActivity)
					((MainActivity) act).startSlaveView();
			}});
		}

		@Override
		public void onDiscoverable()
		{
			handler.post(new Runnable() {public void run()
			{
				if (listenDialog == null)
					return;

				//listenDialog.setStatus("Discoverable");
			}});
		}

		@Override
		public void onFailed()
		{
			handler.post(new Runnable() {public void run()
			{
				if (listenDialog == null || againDialog != null)
					return;

				listenDialog.dismiss();
				listenDialog = null;

				againDialog = new AlertDialog.Builder(getActivity())
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
		MyApplication.getInstance().setupBTServer(new BluetoothCtrl.SetupListener() { public void onSetup()
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

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		listenDialog = builder
				.setTitle("Listening")
				.setMessage(getString(R.string.bluetooth_listen_message))
				.setCancelable(true)
				.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int i)
				{
					btCtrl.cancelListen();
					listenDialog = null;
				}})
				.create();
		listenDialog.show();
	}

	private void thumbnailClicked()
	{
		BaseActivity act = MyApplication.getInstance().getCurrentActivity();
		if (act instanceof MainActivity)
			((MainActivity) act).startThumbnailView();
	}
}

package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.munger.stereocamera.MainActivity;
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

	private DiscoverDialog discoverDialog;
	private HashMap<String, BluetoothDevice> devices;
	private BluetoothDiscoverer discoverer;

	private void connectClicked()
	{
		MainActivity.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
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
			discoverer = MainActivity.getInstance().getBtCtrl().getDiscoverer();

			discoverer.discover(new BluetoothDiscoverer.DiscoverListener()
			{
				public void onDiscovered(BluetoothDevice device)
				{
					String id = device.getAddress();
					String name = device.getName();

					devices.put(id, device);

					discoverDialog.addDiscovery(id, name);
				}
			}, MainActivity.DISCOVER_TIMEOUT);
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
		master = MainActivity.getInstance().getBtCtrl().getMaster();
		master.connect(device, new BluetoothMaster.ConnectListener()
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
					discoverDialog.dismiss();
					MainActivity.getInstance().startMasterView();
				}});
			}
		});
	}


	private ListenDialog listenDialog = null;
	private BluetoothSlave listenSlave = null;

	private void listenClicked()
	{
		if (listenSlave != null)
			return;

		MainActivity.getInstance().setupBTServer(new BluetoothCtrl.SetupListener()
		{
			@Override
			public void onSetup()
			{
				listenClicked2();
			}
		});
	}

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
				listenSlave.cancelListen();
			}
		});

		listenSlave = MainActivity.getInstance().getBtCtrl().getSlave();
		listenSlave.listen(new BluetoothSlave.ListenListener()
		{
			@Override
			public void onAttached()
			{
				handler.post(new Runnable() {public void run()
				{
					listenDialog.setStatus("Connected");
					listenDialog.dismiss();
					MainActivity.getInstance().startSlaveView();
				}});
			}

			@Override
			public void onFailed()
			{
				handler.post(new Runnable() {public void run()
				{
					Toast.makeText(getActivity().getApplicationContext(), R.string.bluetooth_listen_failed_error, Toast.LENGTH_LONG).show();
					listenDialog.dismiss();
				}});
			}
		}, MainActivity.LISTEN_TIMEOUT);
	}
}

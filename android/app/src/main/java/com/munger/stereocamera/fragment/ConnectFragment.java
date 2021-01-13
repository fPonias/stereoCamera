package com.munger.stereocamera.fragment;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.munger.stereocamera.BaseFragment;
import com.munger.stereocamera.BuildConfig;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.command.PhotoOrientation;
import com.munger.stereocamera.utility.data.Client;
import com.munger.stereocamera.utility.data.ClientViewModel;
import com.munger.stereocamera.utility.data.ClientViewModelProvider;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.widget.ThumbnailWidget;

import java.util.HashMap;
import java.util.Set;

public class ConnectFragment extends BaseFragment
{
	Handler handler;
	Preferences prefs;

	private AlertDialog firstTimeDialog;

	private RadioGroup connectChooser;
	private ViewGroup bluetoothControls;
	private ViewGroup wifiControls;
	private ViewGroup testControls;

	private View view;
	private PhotoOrientation orientation;
	//private AdView adView;
	//private InterstitialAd fullAdView;
	private RelativeLayout rootView;
	private ViewGroup buttonsLayout;
	private ThumbnailWidget thumbnail;

	private ConnectBluetoothSubFragment bluetoothCtrl;
	private ConnectEthernetSubFragment ethernetCtrl;
	private TestFragment testCtrl;

	public ConnectFragment()
	{

	}

	private ClientViewModel clientModel;
	private BluetoothCtrl btCtrl;

	public ClientViewModel getClientModel()
	{
		return clientModel;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		btCtrl = MyApplication.getInstance().getBtCtrl();
		clientModel = MainActivity.getInstance().getClientViewModel();

		if (savedInstanceState != null && savedInstanceState.containsKey("adLastShown"))
			lastShown = savedInstanceState.getLong("adLastShown");
		else
			lastShown = 0;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putLong("adLastShown", lastShown);
	}

	MenuItem helpItem;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.connect_menu, menu);
		helpItem = menu.findItem(R.id.help);

		helpItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { public boolean onMenuItemClick(MenuItem item)
		{
			MainActivity.getInstance().openHelp();
			return false;
		}});
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);

		orientation = MainActivity.getInstance().getCurrentOrientation();
		if (orientation.isPortait())
			view = inflater.inflate(R.layout.fragment_connect, container, false);
		else
			view = inflater.inflate(R.layout.fragment_connect_horizontal, container, false);

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

	private boolean isResumed = false;
	private long lastShown = 0;
	private long lastShownThreshold = 6 * 60000;
	private final Object lock = new Object();

	@Override
	public void onResume()
	{
		super.onResume();

		requestPermissions();

		MainActivity activity = MainActivity.getInstance();
		prefs = new Preferences();
		prefs.setup();
		/*if (activity.getAdsEnabled())
		{
			AdRequest adRequest = new AdRequest.Builder().build();
			adView.loadAd(adRequest);
			adRequest = new AdRequest.Builder().build();
			fullAdView.loadAd(adRequest);

			synchronized (lock)
			{
				isResumed = true;
			}

			showFullScreenAd();
		}*/

		boolean firstTime = prefs.getFirstTime();

		if (firstTime)
		{
			doFirstTime();
		}
		//else
		//{
		//	reconnect();
		//}
	}

	private void requestPermissions()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MainActivity.getInstance().requestPermissionForResult(Manifest.permission.READ_EXTERNAL_STORAGE, (result) -> {
				//requestPermissions();
			});

			return;
		}

		thumbnail.update();
	}

	@Override
	public void onStop()
	{
		super.onStop();

		synchronized (lock)
		{
			isResumed = false;
		}
	}

	private void findViews()
	{
		rootView = view.findViewById(R.id.root_view);
		buttonsLayout = view.findViewById(R.id.buttons_layout);

		bluetoothControls = view.findViewById(R.id.bluetoothButtons);
		wifiControls = view.findViewById(R.id.wifiButtons);
		testControls = view.findViewById(R.id.testButtons);

		connectChooser = view.findViewById(R.id.connectChooser);

		thumbnail = view.findViewById(R.id.thumbnail);

		/*MainActivity activity = MainActivity.getInstance();
		if (activity.getAdsEnabled())
		{
			addBannerAd();
			addFullScreenAd();
		}*/
	}

	/*private void addBannerAd()
	{
		MainActivity activity = MainActivity.getInstance();
		adView = new AdView(activity);
		adView.setId(View.generateViewId());
		adView.setAdSize(AdSize.BANNER);

		if (activity.getIsDebug())
			adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
		else
			adView.setAdUnitId("ca-app-pub-9089181112526283/9788729183");

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lp.setMargins(0, 30, 0 , 0);

		adView.setLayoutParams(lp);
		rootView.addView(adView);

		lp = (RelativeLayout.LayoutParams) buttonsLayout.getLayoutParams();
		lp.addRule(RelativeLayout.BELOW, adView.getId());
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		buttonsLayout.setLayoutParams(lp);
	}

	private void addFullScreenAd()
	{
		MainActivity activity = MainActivity.getInstance();
		fullAdView = new InterstitialAd(activity);

		if (activity.getIsDebug())
			fullAdView.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
		else
			fullAdView.setAdUnitId("ca-app-pub-9089181112526283/1107938167");

		fullAdView.setAdListener(new AdListener()
		{
			public void onAdLoaded()
			{
				showFullScreenAd();
			}
		});
	}

	private void showFullScreenAd()
	{
		synchronized (lock)
		{
			if (!fullAdView.isLoaded() || !isResumed)
				return;

			long now = System.currentTimeMillis();
			long diff = now - lastShown;

			if (diff < lastShownThreshold)
				return;

			lastShown = now;
		}

		fullAdView.show();
	}*/

	private void setupViews()
	{
		bluetoothCtrl = new ConnectBluetoothSubFragment(this, bluetoothControls);
		ethernetCtrl = new ConnectEthernetSubFragment(this, wifiControls);

		if (BuildConfig.DEBUG)
		{
			RadioButton testButton = new RadioButton(getContext());
			testButton.setText("Test");
			connectChooser.addView(testButton, 2);
			testCtrl = new TestFragment(this, testControls);
		}

		connectChooser.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				if (checkedId == R.id.connectBluetooth)
				{
					wifiControls.setVisibility(View.GONE);
					testControls.setVisibility(View.GONE);
					ethernetCtrl.cleanUp();
					bluetoothControls.setVisibility(View.VISIBLE);
				}
				else if (checkedId == R.id.connectWifi)
				{
					wifiControls.setVisibility(View.VISIBLE);
					bluetoothControls.setVisibility(View.GONE);
					testControls.setVisibility(View.GONE);
					bluetoothCtrl.cleanUp();
				}
				else
				{
					wifiControls.setVisibility(View.GONE);
					bluetoothControls.setVisibility(View.GONE);
					testControls.setVisibility(View.VISIBLE);
					bluetoothCtrl.cleanUp();
					ethernetCtrl.cleanUp();
				}
			}
		});

		thumbnail.update();
		thumbnail.setOnClickListener(view -> thumbnailClicked());
	}

	private void doFirstTime()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getInstance());
		builder.setTitle(R.string.first_time_title)
				.setMessage(R.string.first_time_question)
				.setNegativeButton(R.string.no_button, (dialog, which) ->
				{
					prefs.setFirstTime(false);
					firstTimeDialog.dismiss();
				})
				.setPositiveButton(R.string.yes_button, (dialog, which) ->
				{
					prefs.setFirstTime(false);
					firstTimeDialog.dismiss();

					MainActivity.getInstance().openHelp();
				});

		firstTimeDialog = builder.create();
		firstTimeDialog.setCanceledOnTouchOutside(false);
		firstTimeDialog.show();
	}

	public void reconnect()
	{
		Thread t = new Thread(() ->
		{
			if (clientModel == null)
				return;

			if (bluetoothControls.getVisibility() == View.VISIBLE)
			{
				reconnect2();
			}
		});
		t.start();
	}

	public void reconnect2()
	{
		Set<BluetoothDevice> devices = btCtrl.getKnownDevices();

		for (BluetoothDevice device : devices)
		{
			String addr = device.getAddress();
			if (!clientModel.has(addr))
				continue;

			Client client = clientModel.get(addr);
			if (client.role == Client.Role.MASTER)
				bluetoothCtrl.connect(client);
			else if (client.role == Client.Role.SLAVE)
				bluetoothCtrl.listen();
		}
	}

	private void thumbnailClicked()
	{
		MainActivity.getInstance().startGalleryView();
	}
}

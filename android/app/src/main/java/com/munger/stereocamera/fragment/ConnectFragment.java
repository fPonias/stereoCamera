package com.munger.stereocamera.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.munger.stereocamera.BaseActivity;
import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;
import com.munger.stereocamera.ip.IPListeners;
import com.munger.stereocamera.ip.bluetooth.BluetoothCtrl;
import com.munger.stereocamera.ip.bluetooth.BluetoothDiscoverer;
import com.munger.stereocamera.ip.bluetooth.BluetoothMaster;
import com.munger.stereocamera.ip.bluetooth.BluetoothSlave;
import com.munger.stereocamera.ip.ethernet.EthernetCtrl;
import com.munger.stereocamera.service.PhotoProcessor;
import com.munger.stereocamera.utility.Preferences;
import com.munger.stereocamera.widget.ThumbnailWidget;

import java.util.ArrayList;
import java.util.HashMap;

public class ConnectFragment extends Fragment
{
	Handler handler;
	Preferences prefs;

	private AlertDialog firstTimeDialog;

	private RadioGroup connectChooser;
	private ViewGroup bluetoothControls;
	private ViewGroup wifiControls;

	private View view;
	private AdView adView;
	private InterstitialAd fullAdView;
	private RelativeLayout rootView;
	private ViewGroup buttonsLayout;
	private ThumbnailWidget thumbnail;

	private ConnectBluetoothSubFragment bluetoothCtrl;
	private ConnectEthernetSubFragment ethernetCtrl;

	public ConnectFragment()
	{

	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null && savedInstanceState.containsKey("adLastShown"))
			lastShown = savedInstanceState.getLong("adLastShown");
		else
			lastShown = 0;

		setHasOptionsMenu(true);
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

	private boolean isResumed = false;
	private long lastShown = 0;
	private long lastShownThreshold = 6 * 60000;
	private final Object lock = new Object();

	@Override
	public void onResume()
	{
		super.onResume();
		thumbnail.update();

		MainActivity activity = MainActivity.getInstance();
		if (activity.getAdsEnabled())
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
		}
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

		connectChooser = view.findViewById(R.id.connectChooser);

		thumbnail = view.findViewById(R.id.thumbnail);

		MainActivity activity = MainActivity.getInstance();
		if (activity.getAdsEnabled())
		{
			addBannerAd();
			addFullScreenAd();
		}
	}

	private void addBannerAd()
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
	}

	private void setupViews()
	{
		prefs = MainActivity.getInstance().getPrefs();
		bluetoothCtrl = new ConnectBluetoothSubFragment(this, bluetoothControls);
		ethernetCtrl = new ConnectEthernetSubFragment(this, wifiControls);

		connectChooser.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				if (checkedId == R.id.connectBluetooth)
				{
					wifiControls.setVisibility(View.GONE);
					ethernetCtrl.cleanUp();
					bluetoothControls.setVisibility(View.VISIBLE);
				}
				else
				{
					wifiControls.setVisibility(View.VISIBLE);
					bluetoothControls.setVisibility(View.GONE);
					bluetoothCtrl.cleanUp();
				}

			}
		});

		thumbnail.update();
		thumbnail.setOnClickListener(new View.OnClickListener() { public void onClick(View view)
		{
			thumbnailClicked();
		}});

		boolean firstTime = prefs.getFirstTime();

		if (firstTime)
		{
			doFirstTime();
		}
	}

	private void doFirstTime()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.first_time_title)
				.setMessage(R.string.first_time_question)
				.setNegativeButton(R.string.no_button, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which)
				{
					prefs.setFirstTime(false);
					firstTimeDialog.dismiss();
				}})
				.setPositiveButton(R.string.yes_button, new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which)
				{
					prefs.setFirstTime(false);
					firstTimeDialog.dismiss();

					MainActivity.getInstance().openHelp();
				}});

		firstTimeDialog = builder.create();
		firstTimeDialog.setCanceledOnTouchOutside(false);
		firstTimeDialog.show();
	}

	public void reconnect()
	{
		//if (prefs.getFirstTime() == true)
		if (true)
			return;

		if (bluetoothControls.getVisibility() == View.VISIBLE)
		{
			Preferences.Roles role = prefs.getRole();
			if (role == Preferences.Roles.MASTER)
			{
				bluetoothCtrl.connect();
			}
			else if (role == Preferences.Roles.SLAVE)
			{
				bluetoothCtrl.listen();
			}
		}
		else
		{

		}
	}

	private void thumbnailClicked()
	{
		MainActivity.getInstance().startGalleryView();
	}
}

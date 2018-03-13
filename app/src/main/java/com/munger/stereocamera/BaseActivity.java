package com.munger.stereocamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;

import com.munger.stereocamera.MyApplication;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class BaseActivity extends AppCompatActivity
{

	@Override
	protected void onResume()
	{
		super.onResume();
		MyApplication.getInstance().setCurrentActivity(this);
	}

	@Override
	protected void onStart()
	{
		MyApplication.getInstance().setCurrentActivity(this);
		super.onStart();
	}

	@Override
	protected void onPause()
	{
		clearActivity();
		super.onPause();
	}

	@Override
	protected void onDestroy()
	{
		clearActivity();
		super.onDestroy();
	}

	protected void clearActivity()
	{
		Activity cur = MyApplication.getInstance().getCurrentActivity();
		if (this.equals(cur))
			MyApplication.getInstance().setCurrentActivity(null);
	}

	public interface ActivityResultListener
	{
		void onResult(int resultCode, Intent data);
	}

	public interface PermissionResultListener
	{
		void onResult(int resultCode);
	}

	private HashMap<Integer, Object> resultListeners = new HashMap<>();

	public void startActivityForResult(Intent i, ActivityResultListener listener)
	{
		int code = (int)(Math.random() * 0x8000);
		resultListeners.put(code, listener);
		startActivityForResult(i, code);
	}

	private HashMap<String, Integer> requestedPermissions = new HashMap<>();
	private final Object lock = new Object();

	public void requestPermissionForResult(String permissions, PermissionResultListener listener)
	{
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			listener.onResult(0);
		else
			requestPermissionForResult2(permissions, listener);
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	private void requestPermissionForResult2(String permissions, PermissionResultListener listener)
	{
		int code;

		synchronized (lock)
		{
			if (requestedPermissions.containsKey(permissions))
			{
				int id = requestedPermissions.get(permissions);
				ArrayList<PermissionResultListener> listeners = (ArrayList<PermissionResultListener>) resultListeners.get(id);
				listeners.add(listener);
				return;
			}

			code = (int) (Math.random() * 0x8000);
			ArrayList<PermissionResultListener> listeners = new ArrayList<>();
			listeners.add(listener);
			requestedPermissions.put(permissions, code);
			resultListeners.put(code, listeners);
		}

		requestPermissions(new String[] {permissions}, code);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		ActivityResultListener listener = (ActivityResultListener) resultListeners.get(requestCode);

		listener.onResult(resultCode, data);
		resultListeners.remove(requestCode);
	}

	@Override
	public void onRequestPermissionsResult(int id, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		ArrayList<PermissionResultListener> listeners;

		synchronized (lock)
		{
			listeners = (ArrayList<PermissionResultListener>) resultListeners.remove(id);
			requestedPermissions.remove(permissions[0]);
		}

		int resultCode = (grantResults.length == 0) ? 0 : grantResults[0];

		for(PermissionResultListener listener : listeners)
			listener.onResult(resultCode);
	}
}

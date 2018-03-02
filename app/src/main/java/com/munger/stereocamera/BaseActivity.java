package com.munger.stereocamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;

import com.munger.stereocamera.MyApplication;

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
	public interface ResultListener
	{
		void onResult(int resultCode, Intent data);
	}

	private HashMap<Integer, ResultListener> resultListeners = new HashMap<>();

	public void startActivityForResult(Intent i, ResultListener listener)
	{
		int code = (int)(Math.random() * 0x8000);
		resultListeners.put(code, listener);
		startActivityForResult(i, code);
	}

	public void requestPermissionForResult(String[] permissions, ResultListener listener)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			int code = (int) (Math.random() * 0x8000);
			resultListeners.put(code, listener);

			requestPermissions(permissions, code);
		}
		else
		{
			listener.onResult(0, null);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		ResultListener listener = resultListeners.get(requestCode);

		listener.onResult(resultCode, data);
		resultListeners.remove(requestCode);
	}
}

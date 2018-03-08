package com.munger.stereocamera.widget;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.bluetooth.command.master.listeners.ReceiveGravity;

public class OrientationCtrl implements SensorEventListener
{
	private SensorManager mgr;
	private Sensor sensor;
	private static long UPDATE_INTERVAL = 100;
	private long lastUpdate = 0;
	private float[] currentValues;

	public OrientationCtrl()
	{
		mgr = (SensorManager) MyApplication.getInstance().getSystemService(Context.SENSOR_SERVICE);
	}

	public void start()
	{
		sensor = mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		mgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	public interface ChangeListener
	{
		void change(float[] values);
	}

	private ChangeListener listener;

	public void setChangeListener(ChangeListener listener)
	{
		this.listener = listener;
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent)
	{
		long now = System.currentTimeMillis();
		long diff = now - lastUpdate;

		if (diff < UPDATE_INTERVAL)
			return;

		currentValues = sensorEvent.values;
		lastUpdate = now;

		if (listener != null)
			listener.change(currentValues);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i)
	{}

	public float[] getValue()
	{
		return currentValues;
	}

	public static double verticalOrientation(float x, float y, float z)
	{
		float zval = x * x + y * y;
		double azrad = Math.atan2(Math.sqrt(zval), z);
		final double az = azrad * 180.0 / Math.PI;

		return az;
	}

	public static double horizontalOrientation(float x, float y, float z)
	{
		float xval = y * y + z * z;
		double axrad = Math.atan2(Math.sqrt(xval), x);
		final double ax = axrad * 180.0 / Math.PI;

		return ax;
	}
}

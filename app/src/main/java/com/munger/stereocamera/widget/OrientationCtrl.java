package com.munger.stereocamera.widget;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.bluetooth.command.PhotoOrientation;
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
		mgr = (SensorManager) MainActivity.getInstance().getSystemService(Context.SENSOR_SERVICE);
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
		return zOrient(x, y, z);
	}

	public static double horizontalOrientation(PhotoOrientation orientation, float x, float y, float z)
	{
		if (orientation == PhotoOrientation.DEG_0)
			return xOrient(x, y, z);
		else if (orientation == PhotoOrientation.DEG_90)
			return -yOrient(x, y, z);
		else if (orientation == PhotoOrientation.DEG_180)
			return -xOrient(x, y, z);
		else
			return yOrient(x, y, z);
	}

	public static double zOrient(float x, float y, float z)
	{
		float zval = x * x + y * y;
		double azrad = Math.atan2(Math.sqrt(zval), z);
		final double az = azrad * 180.0 / Math.PI;

		return az;
	}

	public static double yOrient(float x, float y, float z)
	{
		float yval = x * x + z * z;
		double ayrad = Math.atan2(Math.sqrt(yval), y);
		final double ay = ayrad * 180.0 / Math.PI;

		return ay;
	}

	public static double xOrient(float x, float y, float z)
	{
		float xval = z * z + y * y;
		double axrad = Math.atan2(Math.sqrt(xval), x);
		final double ax = axrad * 180.0 / Math.PI;

		return ax;
	}
}

package com.munger.stereocamera.bluetooth.utility;

import com.munger.stereocamera.MyApplication;
import com.munger.stereocamera.fragment.PreviewFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class CalculateSync
{
	private static int MAX_ATTEMPTS = 10;
	private static long TIMEOUT = 5000;

	private double[] historyInput;
	private double[] historyActual;
	private long currentInput;
	private int currentAttempt;
	private int failCount = 0;

	public interface Listener
	{
		void result(long localDelay);
		void fail();
	}

	private Listener listener;
	private PreviewFragment target;

	public CalculateSync(PreviewFragment fragment, Listener listener)
	{
		this.listener = listener;
		this.target = fragment;

		historyActual = new double[MAX_ATTEMPTS];
		historyInput = new double[MAX_ATTEMPTS];

		currentAttempt = -2;
		currentInput = 0;
	}

	private GetLatency.Listener latencyListener = new GetLatency.Listener()
	{
		@Override
		public void pong(long localLatency, long remoteLatency)
		{
			if (currentAttempt == -1)
			{
				execute();
				return;
			}

			long diff = remoteLatency - localLatency;
			historyInput[currentAttempt] = currentInput;
			historyActual[currentAttempt] = diff;

			currentInput = Math.max(0L, currentInput + diff);
			execute();
		}

		@Override
		public void fail()
		{
			failCount++;

			if (failCount >= 3)
				listener.fail();
			else
				execute();
		}
	};

	public void execute()
	{
		currentAttempt++;

		if (currentAttempt >= MAX_ATTEMPTS)
		{
			long result = calculateResult();
			listener.result(result);
			return;
		}

		RemoteState remoteState = MyApplication.getInstance().getBtCtrl().getMaster().getRemoteState();
		remoteState.waitOnReadyAsync(TIMEOUT, new RemoteState.ReadyListener()
		{
			@Override
			public void done()
			{
				GetLatency getLatency = new GetLatency(latencyListener, target, TIMEOUT);
				getLatency.execute(currentInput);
			}

			@Override
			public void fail()
			{
				listener.fail();
			}
		});
	}

	private long calculateResult()
	{
		try
		{
			loadPastData();
		}
		catch(IOException e){}

		double[] inputs = new double[pastValuesRead + currentAttempt];
		double[] actuals = new double[pastValuesRead + currentAttempt];

		for (int i = 0; i < pastValuesRead; i++)
		{
			inputs[i] = pastInputs[i];
			actuals[i] = pastActuals[i];
		}

		for (int i = 0; i < currentAttempt; i++)
		{
			inputs[i + pastValuesRead] = historyInput[i];
			actuals[i + pastValuesRead] = historyActual[i];
		}

		LinearRegression linreg = new LinearRegression(inputs, actuals);
		double slope = linreg.slope();
		double intercept = linreg.intercept();

		double ret = -intercept / slope;

		try
		{
			saveData(inputs, actuals);
		}
		catch(IOException e){
			int i = 0;
			int j = i;
		}

		return Math.max(0L, (long) ret);
	}

	private int pastValuesRead = 0;
	private double[] pastInputs;
	private double[] pastActuals;

	private void loadPastData() throws IOException
	{
		String key = "syncData-" + MyApplication.getInstance().getPrefs().getClient();
		File file = new File(MyApplication.getInstance().getFilesDir(), key);

		if (!file.exists())
		{
			return;
		}

		byte[] buf = new byte[16];
		FileInputStream fis = new FileInputStream(file);
		ByteBuffer bb = ByteBuffer.wrap(buf);

		int sz;
		int read = fis.read(buf, 0, 4);
		if (read < 4)
			return;

		sz = bb.getInt(0);
		pastInputs = new double[sz];
		pastActuals = new double[sz];
		pastValuesRead = 0;

		for (int i = 0; i < sz; i++)
		{
			read = fis.read(buf, 0, 16);

			if (read < 16)
				return;

			pastInputs[i] = bb.getDouble(0);
			pastActuals[i] = bb.getDouble(8);
			pastValuesRead++;
		}

		fis.close();
	}

	private void saveData(double[] inputs, double[] actuals) throws IOException
	{
		String key = "syncData-" + MyApplication.getInstance().getPrefs().getClient();
		File file = new File(MyApplication.getInstance().getFilesDir(), key);

		if (!file.exists())
		{
			file.createNewFile();
		}

		FileOutputStream fos = new FileOutputStream(file);
		int sz = inputs.length;

		byte[] buf = new byte[16];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.putInt(sz);

		fos.write(buf, 0, 4);

		for (int i = 0; i < sz; i++)
		{
			bb.putDouble(0, inputs[i]);
			bb.putDouble(8, actuals[i]);
			fos.write(buf);
		}

		fos.close();
	}
}

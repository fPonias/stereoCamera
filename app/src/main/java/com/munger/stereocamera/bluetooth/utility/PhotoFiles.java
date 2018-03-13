package com.munger.stereocamera.bluetooth.utility;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.MyApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class PhotoFiles
{
	public PhotoFiles()
	{

	}

	public interface Listener
	{
		void done();
		void fail();
	}

	public void checkPermissions(final Listener listener)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MyApplication.getInstance(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MyApplication.getInstance().getCurrentActivity().requestPermissionForResult(Manifest.permission.WRITE_EXTERNAL_STORAGE, new MainActivity.PermissionResultListener()
			{
				@Override
				public void onResult(int resultCode)
				{
					if (resultCode != -1)
						listener.done();
					else
						listener.fail();
				}
			});
			return;
		}

		listener.done();
	}

	private File targetDir;

	public void openTargetDir(final Listener listener)
	{
		checkPermissions(new Listener()
		{
			@Override
			public void done()
			{
				targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				listener.done();
			}

			@Override
			public void fail()
			{
				listener.fail();
			}
		});
	}

	public int getNewestId()
	{
		String[] files = targetDir.list();
		int max = 0;

		for (String filename : files)
		{
			String[] parts = filename.split("\\.");

			try
			{
				int count = Integer.parseInt(parts[0]);

				if (count > max)
					max = count;
			}
			catch(NumberFormatException e) {}
		}

		return max;
	}

	public File getNewestFile()
	{
		int max = getNewestId();
		return new File(max + ".jpg");
	}

	public File getFile(String name)
	{
		String path = targetDir.getPath() + "/" + name;
		File ret = new File(path);

		if (ret.exists())
			return ret;
		else
			return null;
	}

	public String getFilePath(String name)
	{
		String path = targetDir.getPath() + "/" + name;
		File ret = new File(path);

		if (ret.exists())
			return path;
		else
			return null;
	}

	public ArrayList<String> getFiles()
	{
		ArrayList<String> ret = new ArrayList<>();
		String[] files = targetDir.list();

		for (String file : files)
		{
			if (isFile(file))
			{
				String entry = targetDir.getPath() + "/" + file;
				ret.add(0, entry);
			}
		}

		return ret;
	}

	public boolean hasFiles()
	{
		ArrayList<String> ret = new ArrayList<>();
		String[] files = targetDir.list();

		for (String file : files)
		{
			if (isFile(file))
				return true;
		}

		return false;
	}

	public boolean isFile(String path)
	{
		String[] parts = path.split("\\.");
		if (parts.length == 2)
		{
			try
			{
				Integer.parseInt(parts[0]);

				if (parts[1].equals("jpg"))
				{
					return true;
				}
			}
			catch(Exception e) {}
		}

		return false;
	}

	public String saveNewFile(byte[] data)
	{
		int max = getNewestId();
		max++;

		String localName = max + ".jpg";
		saveFile(localName, data);
		return localName;
	}

	public void saveFile(String name, byte[] data)
	{
		try
		{
			File f = new File(targetDir, name);
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(data);
			fos.close();

			MediaScannerConnection.scanFile(MyApplication.getInstance(), new String[]{f.getPath()}, null, null);
		}
		catch(IOException e){

		}
	}

	public byte[] loadFile(String name)
	{
		byte[] ret = null;
		try
		{
			File f = new File(targetDir, name);
			FileInputStream fis = new FileInputStream(f);

			int sz = (int) f.length();
			ret = new byte[sz];

			int read = 0;
			int total = 0;
			while (total < sz)
			{
				read = fis.read(ret, total, sz - total);
				total += read;
			}
		}
		catch(IOException e){

		}

		return ret;
	}
}

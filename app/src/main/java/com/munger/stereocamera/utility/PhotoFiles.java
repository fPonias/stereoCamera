package com.munger.stereocamera.utility;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Created by hallmarklabs on 3/2/18.
 */

public class PhotoFiles
{
	private Context context;

	public PhotoFiles(Context context)
	{
		this.context = context;
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

	public String saveNewFile(File source)
	{
		int max = getNewestId();
		max++;

		String localName = max + ".jpg";
		File dest = new File(targetDir, localName);
		FileInputStream fis = null;
		FileOutputStream fos = null;
		int total = 0;
		int read = 1;

		try
		{
			fis = new FileInputStream(source);
			fos = new FileOutputStream(dest);

			byte[] buffer = new byte[4096];
			long sz = source.length();

			while (total < sz && read > 0)
			{
				read = fis.read(buffer);

				if (read > 0)
				{
					fos.write(buffer, 0, read);
					total += read;
				}
			}
		}
		catch(IOException e){
			dest = null;
		}
		finally{
			try
			{
				if (fis != null)
					fis.close();
			}
			catch(IOException e){}

			try
			{
				if (fos != null)
					fos.close();
			}
			catch(IOException e){}
		}

		if (dest != null)
			MediaScannerConnection.scanFile(MyApplication.getInstance(), new String[]{dest.getPath()}, null, null);

		return dest.getPath();
	}

	public void saveFile(String name, byte[] data)
	{
		FileOutputStream fos = null;
		try
		{
			File f = new File(targetDir, name);
			fos = new FileOutputStream(f);
			fos.write(data);
			fos.close();

			MediaScannerConnection.scanFile(MyApplication.getInstance(), new String[]{f.getPath()}, null, null);
		}
		catch(IOException e){

		}
		finally{
			try
			{
				if (fos != null)
					fos.close();
			}
			catch(IOException e){}
		}
	}

	public String saveDataToCache(byte[] data)
	{
		String outputPath = null;
		FileOutputStream fos = null;

		try
		{
			File out = getRandomFile();
			fos = new FileOutputStream(out);

			fos.write(data);

			fos.flush();
			outputPath = out.getPath();
		}
		catch(IOException e){

		}
		finally{
			try
			{
				if (fos != null)
					fos.close();
			}
			catch(IOException e){}
		}

		return outputPath;
	}

	public File getRandomFile()
	{
		File f = context.getCacheDir();
		File out = null;
		String path = f.getPath() + "/";

		do
		{
			int id = (int) ((double) Integer.MAX_VALUE * Math.random());
			out = new File(path + id);
		} while (out.exists());

		return out;
	}
}

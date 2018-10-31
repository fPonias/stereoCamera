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
import com.munger.stereocamera.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
		{
			MainActivity.getInstance().requestPermissionForResult(Manifest.permission.WRITE_EXTERNAL_STORAGE, new MainActivity.PermissionResultListener()
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

				if (targetDir == null)
				{
					listener.fail();
					return;
				}

				String subPath = targetDir.getPath() + "/" + context.getResources().getString(R.string.app_name);
				targetDir = new File(subPath);

				if (!targetDir.exists())
					targetDir.mkdirs();

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

	private class FileComparator implements Comparator<String>
	{
		@Override
		public int compare(String o1, String o2)
		{
			int num1 = getFileNumber(o1);
			int num2 = getFileNumber(o2);

			return num2 - num1;
		}

		private int getFileNumber(String file)
		{
			String[] parts = file.split("/");

			if (parts.length == 0)
				return Integer.MAX_VALUE;

			String numStrPart = parts[parts.length - 1];
			int end = numStrPart.indexOf('.');

			if (end == -1)
				return Integer.MAX_VALUE;

			String numStr = numStrPart.substring(0, end);

			try
			{
				int ret = Integer.parseInt(numStr);
				return ret;
			}
			catch(NumberFormatException e){
				return Integer.MAX_VALUE;
			}
		}
	}

	private FileComparator comparator = new FileComparator();

	public String[] getFiles()
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

		int sz = ret.size();
		String[] arr = new String[sz];
		for (int i = 0; i < sz; i++)
			arr[i] = ret.get(i);

		Arrays.sort(arr, comparator);

		return arr;
	}

	public static class DatedFiles
	{
		public ArrayList<Long> dates = new ArrayList<>();
		public HashMap<Long, ArrayList<String>> files = new HashMap<>();
	}

	private Long getIndexFromLong(long dt)
	{
		long ret = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(dt);

		int year = cal.get(Calendar.YEAR);
		ret += year * 10000;

		int month = cal.get(Calendar.MONTH);
		ret += month * 100;

		int day = cal.get(Calendar.DAY_OF_MONTH);
		ret += day;

		return ret;
	}

	public DatedFiles getFilesByDate()
	{
		DatedFiles ret = new DatedFiles();

		long lastIndex = 0;
		ArrayList<String> list = null;
		String[] files = getFiles();
		HashMap<Long, Integer> indexMap = new HashMap<>();

		for (String  fileStr : files)
		{
			File file = new File(fileStr);
			long dt = file.lastModified();
			long index = getIndexFromLong(dt);

			if (lastIndex != index)
			{
				if (!indexMap.containsKey(index))
				{
					int i = ret.dates.size();
					ret.dates.add(index);

					list = new ArrayList<>();
					ret.files.put(index, list);

					indexMap.put(index, i);
				}
				else
				{
					int i = indexMap.get(index);
					long dateIdx = ret.dates.get(i);
					list = ret.files.get(dateIdx);
				}

				lastIndex = index;
			}

			list.add(fileStr);
		}

		int sz = ret.dates.size();
		Long[] keyArr = new Long[sz];
		ret.dates.toArray(keyArr);
		Arrays.sort(keyArr);

		ret.dates = new ArrayList<>();
		for (int i = sz - 1; i >= 0; i--)
			ret.dates.add(keyArr[i]);

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
			MediaScannerConnection.scanFile(MainActivity.getInstance(), new String[]{dest.getPath()}, null, null);

		return dest.getPath();
	}

	public void saveNewFile(byte[] data)
	{
		String localName = getNewFile();
		saveFile(localName, data);
	}

	public String getNewFile()
	{
		int max = getNewestId();
		max++;

		String localName = max + ".jpg";
		return localName;
	}

	public String getNewFilePath()
	{
		String file = getNewFile();
		String path = targetDir + "/" + file;
		return path;
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

			MediaScannerConnection.scanFile(MainActivity.getInstance(), new String[]{f.getPath()}, null, null);
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

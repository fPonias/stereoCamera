package com.munger.stereocamera.utility;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.munger.stereocamera.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

public abstract class PhotoFiles
{
	public static class Factory
	{
		public static PhotoFiles get()
		{
			Context context = MainActivity.getInstance();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				return new PhotoFilesQ(context);
			else
				return new PhotoFilesLegacy(context);
		}
	}

	protected final Context context;
	protected final ContentResolver resolver;
	protected final Uri collection;

	protected PhotoFiles(Context context)
	{
		this.context = context;
		resolver = context.getContentResolver();
		collection = getCollection();
	}

	public interface Listener
	{
		void done();
		void fail();
	}

	public abstract int getNewestId();
	public abstract PhotoFile getNewest();

	public InputStream getStream(Uri uri) throws FileNotFoundException
	{
		return resolver.openInputStream(uri);
	}

	public InputStream getNewestAsStream()
	{
		PhotoFile data = getNewest();
		if (data == null)
			return null;

		try
		{
			return resolver.openInputStream(data.uri);
		}
		catch(Exception e){
			return null;
		}
	}

	public static class DatedFiles
	{
		public ArrayList<Long> dates = new ArrayList<>();
		public HashMap<Long, ArrayList<PhotoFile>> files = new HashMap<>();
	}

	private Long getIndexFromLong(long dt)
	{
		long ret = 0;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(dt * 1000);

		int year = cal.get(Calendar.YEAR);
		ret += year * 10000;

		int month = cal.get(Calendar.MONTH);
		ret += (month + 1) * 100;

		int day = cal.get(Calendar.DAY_OF_MONTH);
		ret += day;

		return ret;
	}

	public DatedFiles getFilesByDate()
	{
		DatedFiles ret = new DatedFiles();
		ArrayList<PhotoFile> data = getAllFiles();

		for (PhotoFile item : data)
		{
			long idx = getIndexFromLong(item.date);

			if (!ret.files.containsKey(idx))
			{
				ret.dates.add(idx);
				ret.files.put(idx, new ArrayList<>());
			}

			ret.files.get(idx).add(item);
		}

		Collections.sort(ret.dates, (l, r) -> (int) (l - r));

		return ret;
	}

	protected abstract Uri getCollection();
	public abstract ArrayList<PhotoFile> getAllFiles();
	public abstract boolean delete(int id);
	public abstract boolean isEmpty();
	protected abstract String getRelativePath();
	public abstract Uri saveFile(File source);

	public String copyAssetToCache(String name)
	{
		String outputPath = null;
		File file = getRandomFile();

		try (InputStream ins = context.getAssets().open(name);
			 FileOutputStream fos = new FileOutputStream(file)
		) {

			byte[] buf = new byte[4096];
			int read;
			while ((read = ins.read(buf)) > 0) {
				fos.write(buf, 0, read);
			}

			outputPath = file.getPath();
		} catch (IOException ignored) {
		}

		return outputPath;
	}

	public String saveDataToCache(byte[] data)
	{
		String outputPath = null;
		File out = getRandomFile();

		try (FileOutputStream fos = new FileOutputStream(out))
		{
			fos.write(data);

			fos.flush();
			outputPath = out.getPath();
		}
		catch(IOException ignored){}

		return outputPath;
	}

	public File getRandomFile()
	{
		File f = context.getCacheDir();
		File out;
		String path = f.getPath() + "/";

		do
		{
			int id = (int) ((double) Integer.MAX_VALUE * Math.random());
			out = new File(path + id);
		} while (out.exists());

		return out;
	}
}

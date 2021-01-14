package com.munger.stereocamera.utility;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
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
	public abstract PhotoFile getFile(int id);

	public InputStream getStream(Uri uri) throws FileNotFoundException
	{
		return resolver.openInputStream(uri);
	}

	public InputStream getStream(int id) throws FileNotFoundException
	{
		PhotoFile file = getFile(id);
		if (file == null)
			throw new FileNotFoundException();

		return getStream(file.uri);
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

	public static class SaveResult
	{
		public Uri uri;
		public int id;
	}

	protected abstract Uri getCollection();
	public abstract ArrayList<PhotoFile> getAllFiles();
	public abstract boolean delete(int id);
	public abstract boolean isEmpty();
	protected abstract String getRelativePath();
	public abstract SaveResult saveFile(File source);
	public abstract long getSize(int id);

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
		} catch (IOException e) {
			int i = 0;
			int j = i;
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

	public Bitmap getThumbnail(int id)
	{
		if (!MainActivity.getInstance().hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
			return null;

		InputStream str;
		try {
			str = getStream(id);

			if (str == null)
				throw new IOException("null stream encountered");
		}
		catch(IOException e){
			return null;
		}

		str.mark(1024 * 1024);
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(str, new Rect(), options);
		double maxw = 512;
		double picw = options.outWidth;
		int skip = (int) Math.ceil(picw / maxw);


		try{
			str.reset();
		}
		catch(IOException e){
			try { str =  getStream(id); } catch(IOException ignored) { return null; }
		}

		options.inSampleSize = skip;
		options.inJustDecodeBounds = false;
		Bitmap bmp = BitmapFactory.decodeStream(str, new Rect(), options);
		return bmp;
	}
}

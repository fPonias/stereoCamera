package com.munger.stereocamera.utility;

import android.content.ContentUris;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.munger.stereocamera.MainActivity;
import com.munger.stereocamera.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class PhotoFilesLegacy extends PhotoFiles {

    public PhotoFilesLegacy(Context context)
    {
        super(context);
    }

    protected Uri getCollection()
    {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }

    public int getNewestId()
    {
        String path = getRelativePath();
        File dir = new File(path);
        File[] files = dir.listFiles();

        if (files == null || files.length == 0)
            return 0;

        File greatest = null;
        for (File file : files)
        {
            if (greatest == null || greatest.lastModified() < file.lastModified())
                greatest = file;
        }

        String[] parts = greatest.getName().split("\\.");
        try {
            return Integer.parseInt(parts[0]);
        }
        catch( NumberFormatException e){
            return 0;
        }
    }

    @Override
    public PhotoFile getNewest()
    {
        int id = getNewestId();
        String path = getRelativePath() + "/" + id + ".jpg";
        return fileToData(new File(path));
    }

    private PhotoFile fileToData(File file)
    {
        String[] parts = file.getName().split("\\.");

        if (parts.length < 2)
            return null;

        int id = Integer.parseInt(parts[parts.length - 2]);
        long date = file.lastModified() / 1000;
        Uri uri = Uri.parse("file://" + getRelativePath() + "/" + file.getName());
        String name = parts[0];
        return new PhotoFile(id, date, name, uri);
    }


    public ArrayList<PhotoFile> getAllFiles()
    {
        ArrayList<PhotoFile> ret = new ArrayList<>();

        String path = getRelativePath();
        File dir = new File(path);
        File[] files = dir.listFiles();

        if (files == null || files.length == 0)
            return ret;

        for (File file : files)
        {
            ret.add(fileToData(file));
        }

        Collections.sort(ret, (o1, o2) -> (int) (o2.date - o1.date));
        return ret;
    }

    public boolean delete(int id)
    {
        Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        int count = resolver.delete(uri, null, null);

        String path = getRelativePath() + "\\" + id + ".jpg";
        File f = new File(path);

        if (f.exists())
            return f.delete();

        return false;
    }

    public boolean isEmpty()
    {
        return (getNewestId() <= 0);
    }

    protected String getRelativePath()
    {
        String suffix = "/" +  context.getResources().getString(R.string.app_name);
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + suffix;
    }

    public Uri saveFile(File source)
    {
        int max = getNewestId();
        max++;

        String localName = max + ".jpg";
        File dest = new File(getRelativePath(), localName);
        int total = 0;
        int read = 1;

        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)
        ) {

            byte[] buffer = new byte[4096];
            long sz = source.length();

            while (total < sz && read > 0) {
                read = fis.read(buffer);

                if (read > 0) {
                    fos.write(buffer, 0, read);
                    total += read;
                }
            }
        } catch (IOException e) {
            dest = null;
        }

        if (dest != null)
            MediaScannerConnection.scanFile(MainActivity.getInstance(), new String[]{dest.getPath()}, null, null);

        return Uri.parse("file://" + dest.getPath());
    }
}

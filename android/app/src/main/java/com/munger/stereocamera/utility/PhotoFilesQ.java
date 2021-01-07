package com.munger.stereocamera.utility;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import com.munger.stereocamera.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class PhotoFilesQ extends PhotoFiles
{
    public PhotoFilesQ(Context context)
    {
        super(context);
    }

    protected Uri getCollection()
    {
        return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }

    public int getNewestId()
    {
        Cursor cursor = resolver.query(collection, new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.OWNER_PACKAGE_NAME + " = ? OR " + MediaStore.Images.Media.OWNER_PACKAGE_NAME + " = ? ",
                new String[]{getPkg(), getPkg() + ".paid"},
                MediaStore.Images.Media._ID + " DESC"
        );

        if (cursor.getCount() == 0)
            return 0;

        cursor.moveToFirst();
        int ret = cursor.getInt(0);
        cursor.close();
        return ret;
    }

    @Override
    public PhotoFile getNewest() {
        Cursor cursor = resolver.query(collection,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DISPLAY_NAME},
                MediaStore.Images.Media.OWNER_PACKAGE_NAME + " = ? OR " + MediaStore.Images.Media.OWNER_PACKAGE_NAME + " = ? ",
                new String[]{getPkg(), getPkg() + ".paid"},
                MediaStore.Images.Media._ID + " DESC"
        );

        if (cursor.getCount() == 0)
            return null;

        cursor.moveToFirst();
        PhotoFile ret = cursorToData(cursor);
        cursor.close();
        return ret;
    }

    public PhotoFile getFile(int id)
    {
        Cursor cursor = resolver.query(collection,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DISPLAY_NAME},
                MediaStore.Images.Media._ID + " = ?",
                new String[] {Integer.toString(id)},
                ""
        );

        if (cursor.getCount() == 0)
            return null;

        cursor.moveToFirst();
        PhotoFile ret = cursorToData(cursor);
        cursor.close();
        return ret;
    }

    public long getSize(int id)
    {
        Cursor cursor = resolver.query(collection,
                new String[] {MediaStore.Images.Media.SIZE},
                MediaStore.Images.Media._ID + " = ?",
                new String[] {Integer.toString(id)},
                "");

        if (cursor.getCount() == 0)
            return -1;

        cursor.moveToFirst();
        long ret = cursor.getLong(0);
        cursor.close();
        return ret;
    }

    private PhotoFile cursorToData(Cursor cursor)
    {
        int id = cursor.getInt(0);
        long date = cursor.getLong(1);
        Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        String name = cursor.getString(2);
        //String pkg = cursor.getString(3);
        return new PhotoFile(id, date, name, uri);
    }


    public ArrayList<PhotoFile> getAllFiles()
    {
        ArrayList<PhotoFile> ret = new ArrayList<>();

        Cursor cursor = resolver.query(collection,
                new String[] {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED, MediaStore.Images.Media.DISPLAY_NAME},
                MediaStore.Images.Media.OWNER_PACKAGE_NAME + " = ? OR " + MediaStore.Images.Media.OWNER_PACKAGE_NAME + " = ? ",
                new String[]{getPkg(), getPkg() + ".paid"},
                MediaStore.Images.Media.DATE_ADDED + " DESC");

        while (cursor.moveToNext())
        {
            ret.add(cursorToData(cursor));
        }

        cursor.close();


        return ret;
    }

    public boolean delete(int id)
    {
        Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        int count = resolver.delete(uri, null, null);

        return (count > 0);
    }

    public boolean isEmpty()
    {
        return (getNewestId() <= 0);
    }

    private String getPkg()
    {
        return context.getPackageName();
    }

    protected String getRelativePath()
    {
        String suffix = "/" +  context.getResources().getString(R.string.app_name);
        return Environment.DIRECTORY_PICTURES + suffix;
    }

    protected SaveResult insertNewFile()
    {
        SaveResult ret = new SaveResult();
        int max = getNewestId();
        max++;

        String localName = max + ".jpg";
        ret.id = max;

        ContentValues details = new ContentValues();
        details.put(MediaStore.Images.Media.DISPLAY_NAME, localName);
        details.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        details.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        details.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        details.put(MediaStore.Images.Media.RELATIVE_PATH, getRelativePath());

        ret.uri = resolver.insert(collection, details);
        return ret;
    }

    public SaveResult saveFile(File source)
    {
        SaveResult ret = insertNewFile();
        int total = 0;
        int read = 1;

        try (
                FileInputStream fis = new FileInputStream(source);
                OutputStream fos = resolver.openOutputStream(ret.uri, "w")
        ) {
            byte[] buffer = new byte[4096];
            long sz = source.length();

            while (total < sz && read > 0)
            {
                read = fis.read(buffer);

                if (read == 0)
                    break;

                fos.write(buffer, 0, read);
                total += read;
            }
        }
        catch (IOException ignored) {}

        return ret;
    }
}

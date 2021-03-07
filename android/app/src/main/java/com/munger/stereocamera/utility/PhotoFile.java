package com.munger.stereocamera.utility;

import android.net.Uri;

public class PhotoFile
{
    public Uri uri;
    public String name;
    public long date;
    public long id;

    public PhotoFile(long id, long date, String name, Uri uri)
    {
        this.id = id;
        this.date = date;
        this.name = name;
        this.uri = uri;
    }
}

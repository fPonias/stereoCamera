package com.munger.stereocamera.utility.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity
public class CameraData
{
    @PrimaryKey (autoGenerate = true)
    public long id;

    @ColumnInfo(name = "clientid")
    public long clientid;

    //@ForeignKey(entity = Client.class, childColumns = "clientid", parentColumns = "id")

    @ColumnInfo(name = "isFacing")
    public boolean isFacing;

    @ColumnInfo(name = "isLeft")
    public boolean isLeft;

    @ColumnInfo(name = "zoom")
    public float zoom;

    @ColumnInfo(name = "isRemote")
    public boolean isRemote;
}

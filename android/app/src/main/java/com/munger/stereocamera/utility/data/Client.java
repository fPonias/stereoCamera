package com.munger.stereocamera.utility.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.munger.stereocamera.widget.PreviewOverlayWidget;
import com.munger.stereocamera.widget.PreviewWidget;

@Entity
public class Client
{
    public enum Role
    {
        MASTER,
        SLAVE,
        NONE
    };

    @PrimaryKey (autoGenerate = true)
    public long id;

    @ColumnInfo(name = "role")
    public Role role;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "lastUsed")
    public long lastUsed;

    @ColumnInfo(name = "delay")
    public long delay;

    @ColumnInfo(name = "isFacing")
    public boolean isFacing;

    @ColumnInfo(name = "overlay")
    public PreviewOverlayWidget.Type overlay;

    @ColumnInfo(name = "resolution")
    public PreviewWidget.ShutterType resolution;
}

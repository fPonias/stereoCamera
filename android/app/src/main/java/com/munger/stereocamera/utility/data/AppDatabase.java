package com.munger.stereocamera.utility.data;

import android.graphics.Camera;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

@Database(entities = {Client.class, CameraData.class}, version = 4, exportSchema = false)
@TypeConverters(TypeConv.class)
public abstract class AppDatabase extends RoomDatabase
{
    public abstract ClientDao clientDao();
    public abstract CameraDataDao cameraDataDao();
}

package com.munger.stereocamera.utility.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CameraDataDao
{
    @Query("SELECT * FROM CameraData WHERE id = :id")
    CameraData get(int id);

    @Query("SELECT * FROM CameraData WHERE clientid = :clientid AND isFacing = :isFacing")
    List<CameraData> getByClient(long clientid, boolean isFacing);

    @Insert
    long insert(CameraData data);

    @Update
    void update(CameraData data);
}

package com.munger.stereocamera.utility.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
import java.util.UUID;

@Dao
public interface ClientDao
{
    @Query("SELECT * FROM client")
    List<Client> getAll();

    @Query("SELECT * FROM client WHERE id = :id")
    Client get(int id);

    @Insert
    long insert(Client client);

    @Update
    void update(Client client);
}

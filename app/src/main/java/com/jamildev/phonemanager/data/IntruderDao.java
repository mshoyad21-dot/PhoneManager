package com.jamildev.phonemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface IntruderDao {
    @Insert
    long insert(IntruderLog log);

    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    LiveData<List<IntruderLog>> observeAll();

    @Query("SELECT * FROM intruder_logs")
    List<IntruderLog> getAllSync();

    @Query("DELETE FROM intruder_logs")
    void clear();
}

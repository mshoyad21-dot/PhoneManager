package com.jamildev.phonemanager.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LockedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(LockedApp app);

    @Query("DELETE FROM locked_apps WHERE packageName = :pkg")
    void delete(String pkg);

    @Query("SELECT * FROM locked_apps WHERE packageName = :pkg")
    LockedApp get(String pkg);

    @Query("SELECT * FROM locked_apps")
    LiveData<List<LockedApp>> observeAll();
}

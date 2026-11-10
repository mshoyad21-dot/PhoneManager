package com.jamildev.phonemanager.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LockedApp.class, IntruderLog.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract LockedAppDao lockedAppDao();

    public abstract IntruderDao intruderDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(), AppDatabase.class, "phonemanager.db").build();
                }
            }
        }
        return instance;
    }
}

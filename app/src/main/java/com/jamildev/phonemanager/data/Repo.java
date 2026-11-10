package com.jamildev.phonemanager.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Thin repository: all writes run on a single background thread. */
public final class Repo {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final AppDatabase db;

    public Repo(Context context) {
        this.db = AppDatabase.get(context);
    }

    public LiveData<List<LockedApp>> observeLockedApps() {
        return db.lockedAppDao().observeAll();
    }

    public LiveData<List<IntruderLog>> observeIntruders() {
        return db.intruderDao().observeAll();
    }

    public void setLocked(String pkg, boolean locked) {
        IO.execute(() -> {
            if (locked) {
                LockedApp existing = db.lockedAppDao().get(pkg);
                db.lockedAppDao().upsert(new LockedApp(pkg, existing != null && existing.hidden));
            } else {
                db.lockedAppDao().delete(pkg);
            }
        });
    }

    /** Hiding always locks the app too; unhiding keeps it locked. */
    public void setHidden(String pkg, boolean hidden) {
        IO.execute(() -> db.lockedAppDao().upsert(new LockedApp(pkg, hidden)));
    }

    public void addIntruder(String pkg, String photoPath) {
        IO.execute(() -> {
            IntruderLog log = new IntruderLog();
            log.packageName = pkg;
            log.photoPath = photoPath;
            log.timestamp = System.currentTimeMillis();
            db.intruderDao().insert(log);
        });
    }

    public void clearIntruders() {
        IO.execute(() -> {
            List<IntruderLog> all = db.intruderDao().getAllSync();
            for (IntruderLog log : all) {
                if (log.photoPath != null) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(log.photoPath).delete();
                }
            }
            db.intruderDao().clear();
        });
    }
}

package com.jamildev.phonemanager.intruder;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.jamildev.phonemanager.data.IntruderLog;
import com.jamildev.phonemanager.data.Repo;

import java.util.List;

public class IntrudersViewModel extends AndroidViewModel {
    private final Repo repo;
    private final LiveData<List<IntruderLog>> logs;

    public IntrudersViewModel(@NonNull Application app) {
        super(app);
        repo = new Repo(app);
        logs = repo.observeIntruders();
    }

    public LiveData<List<IntruderLog>> getLogs() {
        return logs;
    }

    public void clear() {
        repo.clearIntruders();
    }
}

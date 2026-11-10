package com.jamildev.phonemanager.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jamildev.phonemanager.manager.HealthScore;
import com.jamildev.phonemanager.manager.RiskScanner;
import com.jamildev.phonemanager.manager.SystemInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final MutableLiveData<HomeState> state = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application app) {
        super(app);
    }

    public LiveData<HomeState> getState() {
        return state;
    }

    /** Re-reads storage, memory, battery and the app risk review. */
    public void refresh() {
        final Application app = getApplication();
        IO.execute(() -> {
            SystemInfo.Storage storage = SystemInfo.storage();
            SystemInfo.Memory memory = SystemInfo.memory(app);
            SystemInfo.Battery battery = SystemInfo.battery(app);
            int risky = RiskScanner.scan(app, false).size();
            int score = HealthScore.compute(storage, memory, battery, risky);
            state.postValue(new HomeState(score, storage, memory, battery, risky));
        });
    }
}

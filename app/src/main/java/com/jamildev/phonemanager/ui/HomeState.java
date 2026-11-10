package com.jamildev.phonemanager.ui;

import com.jamildev.phonemanager.manager.SystemInfo;

/** Everything the home screen shows, computed off the main thread. */
public class HomeState {
    public final int score;
    public final SystemInfo.Storage storage;
    public final SystemInfo.Memory memory;
    public final SystemInfo.Battery battery;
    public final int riskyApps;

    public HomeState(int score, SystemInfo.Storage storage, SystemInfo.Memory memory,
                     SystemInfo.Battery battery, int riskyApps) {
        this.score = score;
        this.storage = storage;
        this.memory = memory;
        this.battery = battery;
        this.riskyApps = riskyApps;
    }
}

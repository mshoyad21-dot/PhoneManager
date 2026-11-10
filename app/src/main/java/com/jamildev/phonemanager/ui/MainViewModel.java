package com.jamildev.phonemanager.ui;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.jamildev.phonemanager.data.LockedApp;
import com.jamildev.phonemanager.data.Repo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** Combines installed launcher apps with the Room lock/hide state. */
public class MainViewModel extends AndroidViewModel {
    private final Repo repo;
    private final MutableLiveData<List<AppItem>> installed = new MutableLiveData<>();
    private final LiveData<List<LockedApp>> lockedApps;
    private final MediatorLiveData<List<AppItem>> items = new MediatorLiveData<>();

    public MainViewModel(@NonNull Application app) {
        super(app);
        repo = new Repo(app);
        lockedApps = repo.observeLockedApps();

        items.addSource(installed, l -> combine());
        items.addSource(lockedApps, l -> combine());
        loadInstalled();
    }

    public LiveData<List<AppItem>> getItems() {
        return items;
    }

    public void setLocked(AppItem item, boolean locked) {
        repo.setLocked(item.packageName, locked);
    }

    /** Hide if currently visible in the main list, unhide if in the vault. */
    public void toggleHidden(AppItem item) {
        repo.setHidden(item.packageName, !item.hidden);
    }

    private void loadInstalled() {
        final PackageManager pm = getApplication().getPackageManager();
        final String self = getApplication().getPackageName();
        Executors.newSingleThreadExecutor().execute(() -> {
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
            Map<String, AppItem> byPkg = new HashMap<>();
            for (ResolveInfo ri : infos) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(self) || byPkg.containsKey(pkg)) {
                    continue;
                }
                byPkg.put(pkg, new AppItem(pkg, String.valueOf(ri.loadLabel(pm)),
                        ri.loadIcon(pm), false, false));
            }
            List<AppItem> list = new ArrayList<>(byPkg.values());
            Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
            installed.postValue(list);
        });
    }

    private void combine() {
        List<AppItem> base = installed.getValue();
        List<LockedApp> db = lockedApps.getValue();
        if (base == null || db == null) {
            return;
        }
        Map<String, LockedApp> map = new HashMap<>();
        for (LockedApp a : db) {
            map.put(a.packageName, a);
        }
        List<AppItem> out = new ArrayList<>(base.size());
        for (AppItem b : base) {
            LockedApp l = map.get(b.packageName);
            out.add(new AppItem(b.packageName, b.label, b.icon, l != null, l != null && l.hidden));
        }
        items.setValue(out);
    }
}

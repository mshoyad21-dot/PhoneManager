package com.jamildev.phonemanager.monitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.data.AppDatabase;
import com.jamildev.phonemanager.data.LockedApp;
import com.jamildev.phonemanager.lock.LockActivity;
import com.jamildev.phonemanager.security.PermissionHelper;
import com.jamildev.phonemanager.security.Prefs;
import com.jamildev.phonemanager.security.UnlockSession;
import com.jamildev.phonemanager.ui.MainActivity;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Foreground service that watches which app comes to the front (via UsageStats)
 * and shows the lock screen on top of locked apps.
 */
public class MonitorService extends Service {
    private static final String CHANNEL_ID = "monitor";
    private static final int NOTIF_ID = 1;
    private static final long POLL_MS = 500L;

    /** System UI surfaces that must not count as "leaving" an app. */
    private static final Set<String> IGNORED = new HashSet<>();

    static {
        Collections.addAll(IGNORED, "com.android.systemui");
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile Set<String> lockedPackages = new HashSet<>();
    private Observer<List<LockedApp>> lockedObserver;
    private UsageStatsManager usm;
    private String current = "";
    private long lastQueryEnd;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            poll();
            handler.postDelayed(this, POLL_MS);
        }
    };

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Lock everything when the screen turns off.
            UnlockSession.lockAll();
            current = "";
        }
    };

    public static void start(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, MonitorService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, MonitorService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        lastQueryEnd = System.currentTimeMillis() - 5000L;

        createChannel();
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }

        lockedObserver = list -> {
            Set<String> set = new HashSet<>();
            for (LockedApp a : list) {
                set.add(a.packageName);
            }
            lockedPackages = set;
        };
        AppDatabase.get(this).lockedAppDao().observeAll().observeForever(lockedObserver);

        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        handler.post(pollRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        unregisterReceiver(screenOff);
        if (lockedObserver != null) {
            AppDatabase.get(this).lockedAppDao().observeAll().removeObserver(lockedObserver);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    private void poll() {
        if (!PermissionHelper.hasUsageAccess(this)) {
            return;
        }
        long now = System.currentTimeMillis();
        UsageEvents events = usm.queryEvents(lastQueryEnd, now);
        UsageEvents.Event e = new UsageEvents.Event();
        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                onForeground(e.getPackageName());
            }
        }
        lastQueryEnd = now;
    }

    private void onForeground(String pkg) {
        if (pkg == null || pkg.equals(current) || IGNORED.contains(pkg)) {
            return;
        }
        String prev = current;
        current = pkg;

        String self = getPackageName();
        if (!prev.isEmpty() && !prev.equals(self)) {
            UnlockSession.markLeft(prev);
        }
        if (pkg.equals(self) || !lockedPackages.contains(pkg)) {
            return;
        }
        if (UnlockSession.resume(pkg, Prefs.autoLockMs(this))) {
            return;
        }
        Intent i = LockActivity.intent(this, pkg);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), flags);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shield)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pi)
                .build();
    }
}

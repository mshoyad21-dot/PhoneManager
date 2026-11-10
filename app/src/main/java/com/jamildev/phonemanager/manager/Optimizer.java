package com.jamildev.phonemanager.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Optimize": asks Android to stop background processes of other apps and clears this
 * app's own cache. Android decides what is actually stopped, so results vary.
 */
public final class Optimizer {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static final class Result {
        public final long memoryFreed;
        public final long cacheFreed;
        public final int appsStopped;

        Result(long memoryFreed, long cacheFreed, int appsStopped) {
            this.memoryFreed = memoryFreed;
            this.cacheFreed = cacheFreed;
            this.appsStopped = appsStopped;
        }
    }

    public interface Callback {
        void onDone(Result result);
    }

    private Optimizer() {
    }

    /** Runs in the background; the callback is delivered on the main thread. */
    public static void run(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        IO.execute(() -> {
            long before = SystemInfo.memory(app).avail;
            int stopped = killBackgroundApps(app);
            long cache = clearOwnCache(app);
            try {
                Thread.sleep(1200L); // let the system reclaim memory before measuring
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            long after = SystemInfo.memory(app).avail;
            final Result r = new Result(Math.max(0, after - before), cache, stopped);
            MAIN.post(() -> callback.onDone(r));
        });
    }

    private static int killBackgroundApps(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Set<String> done = new HashSet<>();
        for (ResolveInfo ri : apps) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !done.add(pkg)) {
                continue;
            }
            try {
                am.killBackgroundProcesses(pkg);
            } catch (SecurityException ignored) {
                // Permission missing on this device: skip.
            }
        }
        return done.size();
    }

    /** Deletes this app's cache folders and returns the bytes freed. */
    public static long clearOwnCache(Context c) {
        long freed = deleteContents(c.getCacheDir());
        File ext = c.getExternalCacheDir();
        if (ext != null) {
            freed += deleteContents(ext);
        }
        return freed;
    }

    private static long deleteContents(File dir) {
        long freed = 0;
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) {
            return 0;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                freed += deleteContents(f);
            } else {
                long len = f.length();
                if (f.delete()) {
                    freed += len;
                }
            }
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
        return freed;
    }
}

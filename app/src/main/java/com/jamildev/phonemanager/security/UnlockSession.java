package com.jamildev.phonemanager.security;

import android.os.SystemClock;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which locked apps are currently unlocked.
 * Value 0 = app is open now; otherwise the time the user left it (for the auto-lock delay).
 */
public final class UnlockSession {
    private static final ConcurrentHashMap<String, Long> UNLOCKED = new ConcurrentHashMap<>();
    private static volatile boolean selfUnlocked;

    private UnlockSession() {
    }

    public static void unlock(String pkg) {
        UNLOCKED.put(pkg, 0L);
    }

    public static void markLeft(String pkg) {
        Long t = UNLOCKED.get(pkg);
        if (t != null && t == 0L) {
            UNLOCKED.put(pkg, SystemClock.elapsedRealtime());
        }
    }

    /** Called when a locked app comes to the front. True if it may stay open without a PIN. */
    public static boolean resume(String pkg, long delayMs) {
        Long t = UNLOCKED.get(pkg);
        if (t == null) {
            return false;
        }
        if (t == 0L) {
            return true;
        }
        if (SystemClock.elapsedRealtime() - t > delayMs) {
            UNLOCKED.remove(pkg);
            return false;
        }
        UNLOCKED.put(pkg, 0L);
        return true;
    }

    public static void lockAll() {
        UNLOCKED.clear();
    }

    public static boolean isSelfUnlocked() {
        return selfUnlocked;
    }

    public static void setSelfUnlocked(boolean v) {
        selfUnlocked = v;
    }
}

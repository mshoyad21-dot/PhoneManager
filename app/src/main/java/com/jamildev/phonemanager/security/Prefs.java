package com.jamildev.phonemanager.security;

import android.content.Context;
import android.content.SharedPreferences;

/** App settings. */
public final class Prefs {
    private static final String FILE = "pm_prefs";

    /** Values behind the auto-lock spinner, in the same order as R.array.autolock_labels. */
    public static final long[] AUTOLOCK_MS = {0L, 15_000L, 60_000L, 300_000L};

    private Prefs() {
    }

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean randomKeypad(Context c) {
        return sp(c).getBoolean("random_keypad", false);
    }

    public static void setRandomKeypad(Context c, boolean v) {
        sp(c).edit().putBoolean("random_keypad", v).apply();
    }

    public static boolean biometricEnabled(Context c) {
        return sp(c).getBoolean("biometric", true);
    }

    public static void setBiometricEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("biometric", v).apply();
    }

    public static boolean intruderEnabled(Context c) {
        return sp(c).getBoolean("intruder", true);
    }

    public static void setIntruderEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("intruder", v).apply();
    }

    public static long autoLockMs(Context c) {
        return sp(c).getLong("autolock_ms", 0L);
    }

    public static void setAutoLockMs(Context c, long v) {
        sp(c).edit().putLong("autolock_ms", v).apply();
    }

    public static boolean onboardingShown(Context c) {
        return sp(c).getBoolean("onboarding_shown", false);
    }

    public static void setOnboardingShown(Context c, boolean v) {
        sp(c).edit().putBoolean("onboarding_shown", v).apply();
    }

    public static boolean protectionEnabled(Context c) {
        return sp(c).getBoolean("protection", true);
    }

    public static void setProtectionEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("protection", v).apply();
    }
}

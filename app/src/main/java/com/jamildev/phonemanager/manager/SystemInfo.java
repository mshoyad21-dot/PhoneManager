package com.jamildev.phonemanager.manager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;

/** Reads storage, memory and battery numbers from standard Android APIs. */
public final class SystemInfo {
    private SystemInfo() {
    }

    public static final class Storage {
        public final long total;
        public final long free;

        Storage(long total, long free) {
            this.total = total;
            this.free = free;
        }

        public long used() {
            return Math.max(0, total - free);
        }

        public float freeFraction() {
            return total <= 0 ? 1f : (float) free / total;
        }

        public int usedPercent() {
            return total <= 0 ? 0 : Math.round(100f * used() / total);
        }
    }

    public static final class Memory {
        public final long total;
        public final long avail;

        Memory(long total, long avail) {
            this.total = total;
            this.avail = avail;
        }

        public long used() {
            return Math.max(0, total - avail);
        }

        public float freeFraction() {
            return total <= 0 ? 1f : (float) avail / total;
        }

        public int usedPercent() {
            return total <= 0 ? 0 : Math.round(100f * used() / total);
        }
    }

    public static final class Battery {
        public final int level;          // 0-100
        public final boolean charging;
        public final int status;         // BatteryManager.BATTERY_STATUS_*
        public final int health;         // BatteryManager.BATTERY_HEALTH_*
        public final float temperatureC;
        public final int voltageMv;
        public final String technology;

        Battery(int level, boolean charging, int status, int health,
                float temperatureC, int voltageMv, String technology) {
            this.level = level;
            this.charging = charging;
            this.status = status;
            this.health = health;
            this.temperatureC = temperatureC;
            this.voltageMv = voltageMv;
            this.technology = technology;
        }
    }

    public static Storage storage() {
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        return new Storage(fs.getTotalBytes(), fs.getAvailableBytes());
    }

    public static Memory memory(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return new Memory(mi.totalMem, mi.availMem);
    }

    public static Battery battery(Context c) {
        Intent i = c.getApplicationContext()
                .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) {
            return new Battery(0, false, BatteryManager.BATTERY_STATUS_UNKNOWN,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN, 0f, 0, "");
        }
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int pct = scale <= 0 ? 0 : Math.round(100f * level / scale);
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        String tech = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        return new Battery(pct, charging, status,
                i.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
                i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
                i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
                tech == null ? "" : tech);
    }
}

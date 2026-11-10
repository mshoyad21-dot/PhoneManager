package com.jamildev.phonemanager.manager;

import android.os.BatteryManager;

/**
 * Simple, transparent 0-100 score:
 * storage 30% + memory 25% + battery 15% + app risk review 30%.
 * Storage and memory score 100 while at least 30% is free, then fall off linearly.
 */
public final class HealthScore {
    private HealthScore() {
    }

    public static int compute(SystemInfo.Storage s, SystemInfo.Memory m,
                              SystemInfo.Battery b, int riskyApps) {
        double storage = freeScore(s.freeFraction());
        double memory = freeScore(m.freeFraction());
        double battery = b.health == BatteryManager.BATTERY_HEALTH_GOOD
                || b.health == BatteryManager.BATTERY_HEALTH_UNKNOWN ? 100 : 50;
        if (b.temperatureC > 45f) {
            battery = Math.min(battery, 60);
        }
        double security = 100 - 25 * Math.min(riskyApps, 4);
        double score = 0.30 * storage + 0.25 * memory + 0.15 * battery + 0.30 * security;
        return (int) Math.max(0, Math.min(100, Math.round(score)));
    }

    private static double freeScore(float freeFraction) {
        return Math.min(1.0, freeFraction / 0.30) * 100.0;
    }
}

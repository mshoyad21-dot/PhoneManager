package com.jamildev.phonemanager.security;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

public final class PermissionHelper {
    private PermissionHelper() {
    }

    public static boolean hasUsageAccess(Context c) {
        AppOpsManager ops = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (Build.VERSION.SDK_INT >= 29) {
            mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName());
        } else {
            //noinspection deprecation
            mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName());
        }
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return c.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean hasOverlay(Context c) {
        return Settings.canDrawOverlays(c);
    }

    public static boolean hasCamera(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasNotifications(Context c) {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Usage access + overlay are the two needed for the lock to work at all. */
    public static boolean coreReady(Context c) {
        return hasUsageAccess(c) && hasOverlay(c);
    }

    public static Intent usageSettingsIntent() {
        return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
    }

    public static Intent overlaySettingsIntent(Context c) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + c.getPackageName()));
    }
}

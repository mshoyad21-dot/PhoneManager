package com.jamildev.phonemanager.manager;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.Settings;

import com.jamildev.phonemanager.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Permission and setup review of user-installed apps. This is NOT a virus scanner: it only
 * flags things worth a second look (SMS access, accessibility access, device-admin, apps
 * with no install source).
 */
public final class RiskScanner {
    private static final long CACHE_MS = 60_000L;
    private static final List<String> SMS_PERMS = Arrays.asList(
            Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH);

    private static volatile List<Finding> cache;
    private static volatile long cacheTime;

    public static final class Finding {
        public final String packageName;
        public final String label;
        public final Drawable icon;
        /** String resource ids describing why the app is listed. */
        public final List<Integer> reasons;

        Finding(String packageName, String label, Drawable icon, List<Integer> reasons) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
            this.reasons = reasons;
        }
    }

    private RiskScanner() {
    }

    /** Blocking: call from a background thread. */
    public static List<Finding> scan(Context context, boolean force) {
        List<Finding> cached = cache;
        if (!force && cached != null && System.currentTimeMillis() - cacheTime < CACHE_MS) {
            return cached;
        }
        Context c = context.getApplicationContext();
        PackageManager pm = c.getPackageManager();

        Set<String> accessibility = enabledAccessibilityPackages(c);
        Set<String> admins = activeAdminPackages(c);

        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Set<String> seen = new HashSet<>();
        List<Finding> out = new ArrayList<>();

        for (ResolveInfo ri : apps) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) {
                continue;
            }
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue; // preinstalled / updated system apps are skipped
            }
            List<Integer> reasons = new ArrayList<>();
            if (grantsAny(pm, pkg, SMS_PERMS)) {
                reasons.add(R.string.risk_sms);
            }
            if (grantsAny(pm, pkg, Collections.singletonList(Manifest.permission.READ_CALL_LOG))) {
                reasons.add(R.string.risk_calllog);
            }
            if (accessibility.contains(pkg)) {
                reasons.add(R.string.risk_accessibility);
            }
            if (admins.contains(pkg)) {
                reasons.add(R.string.risk_admin);
            }
            if (installerOf(pm, pkg) == null) {
                reasons.add(R.string.risk_unknown_source);
            }
            if (!reasons.isEmpty()) {
                out.add(new Finding(pkg, String.valueOf(ri.loadLabel(pm)), ri.loadIcon(pm), reasons));
            }
        }
        Collections.sort(out, (a, b) -> {
            int d = b.reasons.size() - a.reasons.size();
            return d != 0 ? d : a.label.compareToIgnoreCase(b.label);
        });
        cache = out;
        cacheTime = System.currentTimeMillis();
        return out;
    }

    @SuppressWarnings("deprecation")
    private static boolean grantsAny(PackageManager pm, String pkg, List<String> perms) {
        try {
            PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions == null || pi.requestedPermissionsFlags == null) {
                return false;
            }
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                boolean granted = (pi.requestedPermissionsFlags[i]
                        & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                if (granted && perms.contains(pi.requestedPermissions[i])) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // App disappeared while scanning.
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static String installerOf(PackageManager pm, String pkg) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                return pm.getInstallSourceInfo(pkg).getInstallingPackageName();
            }
            return pm.getInstallerPackageName(pkg);
        } catch (Exception e) {
            return "unknown"; // cannot tell: do not flag
        }
    }

    private static Set<String> enabledAccessibilityPackages(Context c) {
        Set<String> pkgs = new HashSet<>();
        String s = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (s != null) {
            for (String part : s.split(":")) {
                ComponentName cn = ComponentName.unflattenFromString(part);
                if (cn != null) {
                    pkgs.add(cn.getPackageName());
                }
            }
        }
        return pkgs;
    }

    private static Set<String> activeAdminPackages(Context c) {
        Set<String> pkgs = new HashSet<>();
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
            List<ComponentName> admins = dpm.getActiveAdmins();
            if (admins != null) {
                for (ComponentName cn : admins) {
                    pkgs.add(cn.getPackageName());
                }
            }
        } catch (Exception ignored) {
            // Not available on this device.
        }
        return pkgs;
    }
}

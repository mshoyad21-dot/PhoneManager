package com.jamildev.phonemanager.manager;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.jamildev.phonemanager.R;

/** Opens system settings screens safely (some OEMs remove some of them). */
public final class Intents {
    private Intents() {
    }

    public static void open(Context c, Intent intent) {
        try {
            c.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(c, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
    }

    public static void openSettings(Context c, String action) {
        open(c, new Intent(action));
    }

    /** The system "App info" page, where the user can uninstall, clear cache, etc. */
    public static void openAppInfo(Context c, String pkg) {
        open(c, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
    }
}

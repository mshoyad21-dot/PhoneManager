package com.jamildev.phonemanager.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jamildev.phonemanager.security.PermissionHelper;
import com.jamildev.phonemanager.security.PinStore;
import com.jamildev.phonemanager.security.Prefs;

/** Restarts protection after the phone reboots. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (PinStore.isSet(context)
                && Prefs.protectionEnabled(context)
                && PermissionHelper.coreReady(context)) {
            MonitorService.start(context);
        }
    }
}

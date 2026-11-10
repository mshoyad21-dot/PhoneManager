package com.jamildev.phonemanager.lock;

import android.app.Activity;

import com.jamildev.phonemanager.security.PinStore;

public final class PinGate {
    private PinGate() {
    }

    /**
     * If no PIN exists yet, sends the user to PIN creation (which comes back to this screen)
     * and finishes the caller. Returns true when the caller should stop its onCreate.
     */
    public static boolean redirectIfNoPin(Activity a) {
        if (PinStore.isSet(a)) {
            return false;
        }
        a.startActivity(SetupPinActivity.intent(a, a.getClass()));
        a.finish();
        return true;
    }
}

package com.jamildev.phonemanager;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.jamildev.phonemanager.lock.LockActivity;
import com.jamildev.phonemanager.lock.PinProtected;
import com.jamildev.phonemanager.security.PinStore;
import com.jamildev.phonemanager.security.UnlockSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards the PIN-protected screens: whenever one comes to the foreground without
 * being unlocked, the lock screen is shown first.
 */
public class PhoneManagerApp extends Application {
    private static final List<Activity> PROTECTED = new ArrayList<>();

    private int startedCount;

    /** Closes every open PIN-protected screen (used when the user backs out of the lock screen). */
    public static void finishProtectedActivities() {
        for (Activity a : new ArrayList<>(PROTECTED)) {
            a.finish();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, Bundle b) {
                if (a instanceof PinProtected) {
                    PROTECTED.add(a);
                }
            }

            @Override
            public void onActivityDestroyed(Activity a) {
                PROTECTED.remove(a);
            }

            @Override
            public void onActivityStarted(Activity a) {
                if (a instanceof LockActivity) {
                    return;
                }
                startedCount++;
                if (a instanceof PinProtected && PinStore.isSet(a) && !UnlockSession.isSelfUnlocked()) {
                    a.startActivity(LockActivity.intent(a, a.getPackageName()));
                }
            }

            @Override
            public void onActivityStopped(Activity a) {
                if (a instanceof LockActivity) {
                    return;
                }
                startedCount--;
                if (startedCount <= 0 && !a.isChangingConfigurations()) {
                    startedCount = 0;
                    UnlockSession.setSelfUnlocked(false);
                }
            }

            @Override
            public void onActivityResumed(Activity a) {
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }
        });
    }
}

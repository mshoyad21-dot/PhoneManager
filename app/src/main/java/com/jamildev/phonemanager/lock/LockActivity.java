package com.jamildev.phonemanager.lock;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.jamildev.phonemanager.PhoneManagerApp;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.intruder.IntruderCamera;
import com.jamildev.phonemanager.security.PinStore;
import com.jamildev.phonemanager.security.Prefs;
import com.jamildev.phonemanager.security.UnlockSession;
import com.jamildev.phonemanager.ui.PinPadView;

/** The lock screen shown on top of a locked app (or in front of Phone Manager itself). */
public class LockActivity extends AppCompatActivity {
    public static final String EXTRA_PKG = "pkg";

    private static final int SELFIE_AFTER_WRONG = 2;
    private static final int LOCKOUT_AFTER_WRONG = 5;
    private static final int LOCKOUT_SECONDS = 30;

    private String targetPkg;
    private int wrongCount;
    private boolean promptBiometricOnResume;
    private boolean biometricAvailable;
    private CountDownTimer lockoutTimer;

    private PinPadView pad;
    private TextView status;
    private TextView targetName;
    private ImageView targetIcon;
    private MaterialButton btnBiometric;

    public static Intent intent(Context c, String pkg) {
        return new Intent(c, LockActivity.class).putExtra(EXTRA_PKG, pkg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        pad = findViewById(R.id.pinPad);
        status = findViewById(R.id.status);
        targetName = findViewById(R.id.targetName);
        targetIcon = findViewById(R.id.targetIcon);
        btnBiometric = findViewById(R.id.btnBiometric);

        pad.setRandomized(Prefs.randomKeypad(this));
        pad.setListener(this::onPin);

        biometricAvailable = Prefs.biometricEnabled(this)
                && BiometricManager.from(this).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
        btnBiometric.setVisibility(biometricAvailable ? View.VISIBLE : View.GONE);
        btnBiometric.setOnClickListener(v -> showBiometric());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });

        loadTarget(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        wrongCount = 0;
        loadTarget(intent);
        pad.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (promptBiometricOnResume && biometricAvailable) {
            promptBiometricOnResume = false;
            showBiometric();
        }
    }

    @Override
    protected void onDestroy() {
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
        }
        super.onDestroy();
    }

    private void loadTarget(Intent intent) {
        targetPkg = intent.getStringExtra(EXTRA_PKG);
        if (targetPkg == null) {
            targetPkg = getPackageName();
        }
        PackageManager pm = getPackageManager();
        try {
            targetName.setText(pm.getApplicationLabel(pm.getApplicationInfo(targetPkg, 0)));
            targetIcon.setImageDrawable(pm.getApplicationIcon(targetPkg));
        } catch (PackageManager.NameNotFoundException e) {
            targetName.setText(R.string.app_name);
        }
        status.setText(R.string.lock_enter);
        promptBiometricOnResume = true;
    }

    private void onPin(String pin) {
        if (PinStore.verify(this, pin)) {
            unlocked();
            return;
        }
        wrongCount++;
        status.setText(R.string.lock_wrong);
        pad.clear();

        if (wrongCount >= SELFIE_AFTER_WRONG && Prefs.intruderEnabled(this)) {
            IntruderCamera.capture(this, this, targetPkg);
        }
        if (wrongCount >= LOCKOUT_AFTER_WRONG) {
            startLockout();
        }
    }

    private void startLockout() {
        pad.setPadEnabled(false);
        btnBiometric.setEnabled(false);
        lockoutTimer = new CountDownTimer(LOCKOUT_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long msLeft) {
                status.setText(getString(R.string.lock_lockout, (int) (msLeft / 1000L) + 1));
            }

            @Override
            public void onFinish() {
                wrongCount = 0;
                pad.setPadEnabled(true);
                btnBiometric.setEnabled(true);
                status.setText(R.string.lock_enter);
            }
        }.start();
    }

    private void showBiometric() {
        BiometricPrompt prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        unlocked();
                    }
                });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setNegativeButtonText(getString(R.string.biometric_cancel))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();
        prompt.authenticate(info);
    }

    private void unlocked() {
        if (targetPkg.equals(getPackageName())) {
            UnlockSession.setSelfUnlocked(true);
        } else {
            UnlockSession.unlock(targetPkg);
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private void goHome() {
        if (targetPkg.equals(getPackageName())) {
            // Our own protected screens: close them and fall back to the Phone Manager home.
            PhoneManagerApp.finishProtectedActivities();
            finish();
            return;
        }
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(home);
        finish();
    }
}

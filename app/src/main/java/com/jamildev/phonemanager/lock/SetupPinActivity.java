package com.jamildev.phonemanager.lock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.security.PinStore;
import com.jamildev.phonemanager.security.UnlockSession;
import com.jamildev.phonemanager.ui.PinPadView;

/** PIN creation. Used the first time App Lock is opened, and for "Change PIN". */
public class SetupPinActivity extends AppCompatActivity {
    private static final String EXTRA_NEXT = "next";

    private PinPadView pad;
    private TextView status;
    private TextView step;
    private String firstPin;
    private String nextClassName;

    /** @param next screen to open once the PIN is saved, or null to just go back. */
    public static Intent intent(Context c, @Nullable Class<?> next) {
        Intent i = new Intent(c, SetupPinActivity.class);
        if (next != null) {
            i.putExtra(EXTRA_NEXT, next.getName());
        }
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_pin);
        nextClassName = getIntent().getStringExtra(EXTRA_NEXT);

        pad = findViewById(R.id.setupPad);
        status = findViewById(R.id.setupStatus);
        step = findViewById(R.id.setupStep);
        pad.setListener(this::onPin);
    }

    private void onPin(String pin) {
        if (firstPin == null) {
            firstPin = pin;
            step.setText(R.string.setup_step2);
            status.setText(R.string.setup_confirm);
            pad.clear();
            return;
        }
        if (!firstPin.equals(pin)) {
            firstPin = null;
            step.setText(R.string.setup_step1);
            status.setText(R.string.setup_mismatch);
            pad.clear();
            return;
        }
        PinStore.setPin(this, pin);
        UnlockSession.setSelfUnlocked(true);
        Toast.makeText(this, R.string.setup_done, Toast.LENGTH_SHORT).show();
        if (nextClassName != null) {
            try {
                startActivity(new Intent(this, Class.forName(nextClassName)));
            } catch (ClassNotFoundException ignored) {
                // Nothing to open; just close.
            }
        }
        finish();
    }
}

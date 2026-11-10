package com.jamildev.phonemanager.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.lock.PinGate;
import com.jamildev.phonemanager.lock.PinProtected;

import com.jamildev.phonemanager.intruder.IntrudersActivity;
import com.jamildev.phonemanager.lock.SetupPinActivity;
import com.jamildev.phonemanager.security.Prefs;

public class SettingsActivity extends AppCompatActivity implements PinProtected {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PinGate.redirectIfNoPin(this)) {
            return;
        }
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.settings_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        MaterialSwitch swRandom = findViewById(R.id.swRandom);
        MaterialSwitch swBiometric = findViewById(R.id.swBiometric);
        MaterialSwitch swIntruder = findViewById(R.id.swIntruder);
        Spinner spAutoLock = findViewById(R.id.spAutoLock);

        swRandom.setChecked(Prefs.randomKeypad(this));
        swRandom.setOnCheckedChangeListener((b, v) -> Prefs.setRandomKeypad(this, v));

        swBiometric.setChecked(Prefs.biometricEnabled(this));
        swBiometric.setOnCheckedChangeListener((b, v) -> Prefs.setBiometricEnabled(this, v));

        swIntruder.setChecked(Prefs.intruderEnabled(this));
        swIntruder.setOnCheckedChangeListener((b, v) -> Prefs.setIntruderEnabled(this, v));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.autolock_labels, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spAutoLock.setAdapter(adapter);
        long current = Prefs.autoLockMs(this);
        for (int i = 0; i < Prefs.AUTOLOCK_MS.length; i++) {
            if (Prefs.AUTOLOCK_MS[i] == current) {
                spAutoLock.setSelection(i);
            }
        }
        spAutoLock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Prefs.setAutoLockMs(SettingsActivity.this, Prefs.AUTOLOCK_MS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        findViewById(R.id.btnChangePin).setOnClickListener(
                v -> startActivity(SetupPinActivity.intent(this, null)));
        findViewById(R.id.btnIntruders).setOnClickListener(
                v -> startActivity(new Intent(this, IntrudersActivity.class)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

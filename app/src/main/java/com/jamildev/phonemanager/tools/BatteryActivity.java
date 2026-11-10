package com.jamildev.phonemanager.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.manager.Intents;
import com.jamildev.phonemanager.manager.SystemInfo;

import java.util.Locale;

public class BatteryActivity extends AppCompatActivity {
    private TextView level;
    private LinearProgressIndicator progress;
    private View rowStatus;
    private View rowHealth;
    private View rowTemp;
    private View rowVoltage;
    private View rowTech;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            render();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battery);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.tool_battery);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        level = findViewById(R.id.battLevel);
        progress = findViewById(R.id.battProgress);
        rowStatus = findViewById(R.id.rowStatus);
        rowHealth = findViewById(R.id.rowHealth);
        rowTemp = findViewById(R.id.rowTemp);
        rowVoltage = findViewById(R.id.rowVoltage);
        rowTech = findViewById(R.id.rowTech);

        findViewById(R.id.btnSaver).setOnClickListener(
                v -> Intents.openSettings(this, Settings.ACTION_BATTERY_SAVER_SETTINGS));
        findViewById(R.id.btnUsage).setOnClickListener(
                v -> Intents.openSettings(this, Intent.ACTION_POWER_USAGE_SUMMARY));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        render();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void render() {
        SystemInfo.Battery b = SystemInfo.battery(this);
        level.setText(String.format(Locale.getDefault(), "%d%%", b.level));
        progress.setProgressCompat(b.level, true);

        setRow(rowStatus, R.string.batt_status, getString(statusRes(b.status)));
        setRow(rowHealth, R.string.batt_health, getString(healthRes(b.health)));
        setRow(rowTemp, R.string.batt_temp, String.format(Locale.getDefault(), "%.1f °C", b.temperatureC));
        setRow(rowVoltage, R.string.batt_voltage, String.format(Locale.getDefault(), "%.2f V", b.voltageMv / 1000f));
        setRow(rowTech, R.string.batt_tech, b.technology.isEmpty() ? "–" : b.technology);
    }

    private void setRow(View row, int labelRes, String value) {
        ((TextView) row.findViewById(R.id.infoLabel)).setText(labelRes);
        ((TextView) row.findViewById(R.id.infoValue)).setText(value);
    }

    private static int statusRes(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return R.string.batt_status_charging;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return R.string.batt_status_discharging;
            case BatteryManager.BATTERY_STATUS_FULL:
                return R.string.batt_status_full;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return R.string.batt_status_not_charging;
            default:
                return R.string.batt_unknown;
        }
    }

    private static int healthRes(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return R.string.batt_health_good;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return R.string.batt_health_overheat;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return R.string.batt_health_dead;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return R.string.batt_health_over_voltage;
            case BatteryManager.BATTERY_HEALTH_COLD:
                return R.string.batt_health_cold;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return R.string.batt_health_failure;
            default:
                return R.string.batt_unknown;
        }
    }
}

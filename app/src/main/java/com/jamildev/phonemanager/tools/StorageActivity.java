package com.jamildev.phonemanager.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.manager.Intents;
import com.jamildev.phonemanager.manager.Optimizer;
import com.jamildev.phonemanager.manager.SystemInfo;

public class StorageActivity extends AppCompatActivity {
    private TextView summary;
    private LinearProgressIndicator bar;
    private View rowUsed;
    private View rowFree;
    private View rowTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.tool_storage);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        summary = findViewById(R.id.storageSummary);
        bar = findViewById(R.id.storageBar);
        rowUsed = findViewById(R.id.rowUsed);
        rowFree = findViewById(R.id.rowFree);
        rowTotal = findViewById(R.id.rowTotal);

        findViewById(R.id.btnLargest).setOnClickListener(
                v -> startActivity(new Intent(this, AppManagerActivity.class)));
        findViewById(R.id.btnSystemStorage).setOnClickListener(
                v -> Intents.openSettings(this, Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        findViewById(R.id.btnClearCache).setOnClickListener(v -> clearCache());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
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
        SystemInfo.Storage s = SystemInfo.storage();
        summary.setText(getString(R.string.storage_used_of,
                Formatter.formatShortFileSize(this, s.used()),
                Formatter.formatShortFileSize(this, s.total)));
        bar.setProgressCompat(s.usedPercent(), true);
        setRow(rowUsed, R.string.storage_row_used, Formatter.formatShortFileSize(this, s.used()));
        setRow(rowFree, R.string.storage_row_free, Formatter.formatShortFileSize(this, s.free));
        setRow(rowTotal, R.string.storage_row_total, Formatter.formatShortFileSize(this, s.total));
    }

    private void setRow(View row, int labelRes, String value) {
        ((TextView) row.findViewById(R.id.infoLabel)).setText(labelRes);
        ((TextView) row.findViewById(R.id.infoValue)).setText(value);
    }

    private void clearCache() {
        final Context app = getApplicationContext();
        new Thread(() -> {
            final long freed = Optimizer.clearOwnCache(app);
            runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.storage_cache_cleared, Formatter.formatShortFileSize(this, freed)),
                    Toast.LENGTH_SHORT).show());
        }).start();
    }
}

package com.jamildev.phonemanager.tools;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.storage.StorageManager;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.manager.Intents;
import com.jamildev.phonemanager.security.PermissionHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Installed apps with their size. Tap an app to open its system App info page. */
public class AppManagerActivity extends AppCompatActivity {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static class Row {
        final String pkg;
        final String label;
        final Drawable icon;
        final long size; // -1 = unknown

        Row(String pkg, String label, Drawable icon, long size) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
            this.size = size;
        }
    }

    private RowAdapter adapter;
    private View banner;
    private View loading;
    private List<Row> rows = new ArrayList<>();
    private boolean sortBySize = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.tool_app_manager);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        banner = findViewById(R.id.usageBanner);
        loading = findViewById(R.id.appManagerLoading);
        findViewById(R.id.btnGrantUsage).setOnClickListener(
                v -> Intents.open(this, PermissionHelper.usageSettingsIntent()));

        adapter = new RowAdapter();
        RecyclerView list = findViewById(R.id.appManagerList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        ChipGroup sort = findViewById(R.id.sortGroup);
        sort.setOnCheckedStateChangeListener((g, ids) -> {
            sortBySize = ids.contains(R.id.chipSize);
            showSorted();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void load() {
        final Context app = getApplicationContext();
        final boolean canMeasure = Build.VERSION.SDK_INT >= 26 && PermissionHelper.hasUsageAccess(app);
        banner.setVisibility(canMeasure || Build.VERSION.SDK_INT < 26 ? View.GONE : View.VISIBLE);
        loading.setVisibility(View.VISIBLE);

        IO.execute(() -> {
            PackageManager pm = app.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
            Set<String> seen = new HashSet<>();
            List<Row> out = new ArrayList<>();
            for (ResolveInfo ri : infos) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(app.getPackageName()) || !seen.add(pkg)) {
                    continue;
                }
                long size = canMeasure ? measure(app, pkg) : -1;
                out.add(new Row(pkg, String.valueOf(ri.loadLabel(pm)), ri.loadIcon(pm), size));
            }
            runOnUiThread(() -> {
                if (isDestroyed()) {
                    return;
                }
                rows = out;
                loading.setVisibility(View.GONE);
                showSorted();
            });
        });
    }

    private static long measure(Context c, String pkg) {
        if (Build.VERSION.SDK_INT < 26) {
            return -1;
        }
        try {
            StorageStatsManager ssm = (StorageStatsManager) c.getSystemService(Context.STORAGE_STATS_SERVICE);
            StorageStats st = ssm.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg, Process.myUserHandle());
            return st.getAppBytes() + st.getDataBytes() + st.getCacheBytes();
        } catch (Exception e) {
            return -1;
        }
    }

    private void showSorted() {
        List<Row> sorted = new ArrayList<>(rows);
        if (sortBySize) {
            Collections.sort(sorted, (a, b) -> Long.compare(b.size, a.size));
        } else {
            Collections.sort(sorted, (a, b) -> a.label.compareToIgnoreCase(b.label));
        }
        adapter.submitList(sorted);
    }

    private class RowAdapter extends ListAdapter<Row, RowAdapter.VH> {
        RowAdapter() {
            super(new DiffUtil.ItemCallback<Row>() {
                @Override
                public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                    return a.pkg.equals(b.pkg);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                    return a.size == b.size;
                }
            });
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app_size, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final Row r = getItem(position);
            h.icon.setImageDrawable(r.icon);
            h.label.setText(r.label);
            h.size.setText(r.size < 0 ? "–" : Formatter.formatShortFileSize(AppManagerActivity.this, r.size));
            h.itemView.setOnClickListener(v -> Intents.openAppInfo(AppManagerActivity.this, r.pkg));
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView label;
            final TextView size;

            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.sizeIcon);
                label = v.findViewById(R.id.sizeLabel);
                size = v.findViewById(R.id.sizeValue);
            }
        }
    }
}

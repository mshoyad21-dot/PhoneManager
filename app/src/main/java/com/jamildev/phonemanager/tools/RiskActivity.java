package com.jamildev.phonemanager.tools;

import android.os.Bundle;
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

import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.manager.Intents;
import com.jamildev.phonemanager.manager.RiskScanner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lists user-installed apps worth a second look. Tap one to open its App info page. */
public class RiskActivity extends AppCompatActivity {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private FindingAdapter adapter;
    private View loading;
    private View empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.tool_risk);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loading = findViewById(R.id.riskLoading);
        empty = findViewById(R.id.riskEmpty);
        adapter = new FindingAdapter();
        RecyclerView list = findViewById(R.id.riskList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loading.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        IO.execute(() -> {
            final List<RiskScanner.Finding> found = RiskScanner.scan(getApplicationContext(), true);
            runOnUiThread(() -> {
                if (isDestroyed()) {
                    return;
                }
                loading.setVisibility(View.GONE);
                adapter.submitList(found);
                empty.setVisibility(found.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class FindingAdapter extends ListAdapter<RiskScanner.Finding, FindingAdapter.VH> {
        FindingAdapter() {
            super(new DiffUtil.ItemCallback<RiskScanner.Finding>() {
                @Override
                public boolean areItemsTheSame(@NonNull RiskScanner.Finding a, @NonNull RiskScanner.Finding b) {
                    return a.packageName.equals(b.packageName);
                }

                @Override
                public boolean areContentsTheSame(@NonNull RiskScanner.Finding a, @NonNull RiskScanner.Finding b) {
                    return a.reasons.equals(b.reasons);
                }
            });
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_risk, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final RiskScanner.Finding f = getItem(position);
            h.icon.setImageDrawable(f.icon);
            h.label.setText(f.label);
            StringBuilder sb = new StringBuilder();
            for (int res : f.reasons) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append("• ").append(getString(res));
            }
            h.reasons.setText(sb);
            h.itemView.setOnClickListener(v -> Intents.openAppInfo(RiskActivity.this, f.packageName));
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView label;
            final TextView reasons;

            VH(@NonNull View v) {
                super(v);
                icon = v.findViewById(R.id.riskIcon);
                label = v.findViewById(R.id.riskLabel);
                reasons = v.findViewById(R.id.riskReasons);
            }
        }
    }
}

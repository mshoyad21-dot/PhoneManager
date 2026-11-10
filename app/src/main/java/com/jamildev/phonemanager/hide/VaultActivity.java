package com.jamildev.phonemanager.hide;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.lock.PinGate;
import com.jamildev.phonemanager.lock.PinProtected;

import com.jamildev.phonemanager.ui.AppAdapter;
import com.jamildev.phonemanager.ui.AppItem;
import com.jamildev.phonemanager.ui.MainViewModel;

import java.util.ArrayList;
import java.util.List;

/** Lists hidden apps. Tapping one opens it (the lock screen still appears first). */
public class VaultActivity extends AppCompatActivity implements AppAdapter.Listener, PinProtected {
    private MainViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PinGate.redirectIfNoPin(this)) {
            return;
        }
        setContentView(R.layout.activity_vault);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.vault_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final AppAdapter adapter = new AppAdapter(true, this);
        RecyclerView list = findViewById(R.id.vaultList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        final TextView empty = findViewById(R.id.vaultEmpty);

        vm = new ViewModelProvider(this).get(MainViewModel.class);
        vm.getItems().observe(this, items -> {
            List<AppItem> hidden = new ArrayList<>();
            for (AppItem i : items) {
                if (i.hidden) {
                    hidden.add(i);
                }
            }
            adapter.submitList(hidden);
            empty.setVisibility(hidden.isEmpty() ? View.VISIBLE : View.GONE);
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

    @Override
    public void onHideClicked(AppItem item) {
        vm.toggleHidden(item); // unhide
    }

    @Override
    public void onOpen(AppItem item) {
        Intent launch = getPackageManager().getLaunchIntentForPackage(item.packageName);
        if (launch == null) {
            Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(launch);
    }

    @Override
    public void onLockChanged(AppItem item, boolean locked) {
        // Switch is hidden in the vault.
    }
}

package com.jamildev.phonemanager.lock;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.hide.VaultActivity;
import com.jamildev.phonemanager.intruder.IntrudersActivity;
import com.jamildev.phonemanager.ui.AppAdapter;
import com.jamildev.phonemanager.ui.AppItem;
import com.jamildev.phonemanager.ui.MainViewModel;
import com.jamildev.phonemanager.ui.SettingsActivity;
import com.jamildev.phonemanager.monitor.MonitorService;
import com.jamildev.phonemanager.security.PermissionHelper;
import com.jamildev.phonemanager.security.Prefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppLockActivity extends AppCompatActivity implements AppAdapter.Listener, PinProtected {
    private MainViewModel vm;
    private AppAdapter adapter;

    private View setupCard;
    private TextView setupTitle;
    private View stepUsage;
    private View stepOverlay;
    private View stepCamera;
    private View stepNotif;
    private TextView statusTitle;
    private TextView statusSubtitle;
    private MaterialSwitch switchProtection;
    private View loading;
    private View emptyView;

    private List<AppItem> allItems = new ArrayList<>();
    private boolean itemsLoaded;
    private boolean onlyLocked;
    private String query = "";

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> refreshState());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PinGate.redirectIfNoPin(this)) {
            return;
        }
        setContentView(R.layout.activity_app_lock);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(R.string.lock_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setupCard = findViewById(R.id.setupCard);
        setupTitle = findViewById(R.id.setupTitle);
        stepUsage = findViewById(R.id.stepUsage);
        stepOverlay = findViewById(R.id.stepOverlay);
        stepCamera = findViewById(R.id.stepCamera);
        stepNotif = findViewById(R.id.stepNotif);
        statusTitle = findViewById(R.id.statusTitle);
        statusSubtitle = findViewById(R.id.statusSubtitle);
        switchProtection = findViewById(R.id.switchProtection);
        loading = findViewById(R.id.loading);
        emptyView = findViewById(R.id.emptyView);

        switchProtection.setChecked(Prefs.protectionEnabled(this));
        switchProtection.setOnCheckedChangeListener((b, checked) -> {
            Prefs.setProtectionEnabled(this, checked);
            refreshState();
        });

        adapter = new AppAdapter(false, this);
        RecyclerView list = findViewById(R.id.appList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        ((android.widget.EditText) findViewById(R.id.search)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString().trim();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ChipGroup chips = findViewById(R.id.chipGroup);
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            onlyLocked = ids.contains(R.id.chipLocked);
            applyFilter();
        });

        vm = new ViewModelProvider(this).get(MainViewModel.class);
        vm.getItems().observe(this, items -> {
            allItems = items;
            itemsLoaded = true;
            applyFilter();
            updateStatus();
        });

        showOnboardingOnce();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (switchProtection != null) {
            refreshState();
        }
    }

    private void showOnboardingOnce() {
        if (Prefs.onboardingShown(this)) {
            return;
        }
        Prefs.setOnboardingShown(this, true);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.onboarding_title)
                .setMessage(R.string.onboarding_body)
                .setPositiveButton(R.string.onboarding_ok, null)
                .show();
    }

    /** Shows only the apps that match the search text and the All/Locked chip. */
    private void applyFilter() {
        String q = query.toLowerCase(Locale.getDefault());
        List<AppItem> out = new ArrayList<>();
        for (AppItem i : allItems) {
            if (i.hidden || (onlyLocked && !i.locked)) {
                continue;
            }
            if (!q.isEmpty() && !i.label.toLowerCase(Locale.getDefault()).contains(q)) {
                continue;
            }
            out.add(i);
        }
        adapter.submitList(out);
        loading.setVisibility(itemsLoaded ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(itemsLoaded && out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Updates the checklist and status card, and starts/stops the monitor service. */
    private void refreshState() {
        updateSetupCard();
        updateStatus();
        if (Prefs.protectionEnabled(this) && PermissionHelper.coreReady(this)) {
            MonitorService.start(this);
        } else {
            MonitorService.stop(this);
        }
    }

    private void updateStatus() {
        boolean enabled = Prefs.protectionEnabled(this);
        if (!enabled) {
            statusTitle.setText(R.string.status_off);
            statusSubtitle.setText(R.string.status_turn_on);
        } else if (!PermissionHelper.coreReady(this)) {
            statusTitle.setText(R.string.status_setup);
            statusSubtitle.setText(R.string.status_grant);
        } else {
            int locked = 0;
            for (AppItem i : allItems) {
                if (i.locked) {
                    locked++;
                }
            }
            statusTitle.setText(R.string.status_on);
            statusSubtitle.setText(getString(R.string.status_locked_count, locked));
        }
    }

    private void updateSetupCard() {
        boolean notifNeeded = Build.VERSION.SDK_INT >= 33;
        boolean usage = PermissionHelper.hasUsageAccess(this);
        boolean overlay = PermissionHelper.hasOverlay(this);
        boolean camera = PermissionHelper.hasCamera(this);
        boolean notif = !notifNeeded || PermissionHelper.hasNotifications(this);

        bindStep(stepUsage, usage, R.string.perm_usage_title, R.string.perm_usage_desc, false,
                v -> startActivity(PermissionHelper.usageSettingsIntent()));
        bindStep(stepOverlay, overlay, R.string.perm_overlay_title, R.string.perm_overlay_desc, false,
                v -> startActivity(PermissionHelper.overlaySettingsIntent(this)));
        bindStep(stepCamera, camera, R.string.perm_camera_title, R.string.perm_camera_desc, true,
                v -> permissionLauncher.launch(Manifest.permission.CAMERA));
        bindStep(stepNotif, notif, R.string.perm_notif_title, R.string.perm_notif_desc, true,
                v -> {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                });

        int total = notifNeeded ? 4 : 3;
        int done = (usage ? 1 : 0) + (overlay ? 1 : 0) + (camera ? 1 : 0)
                + (notifNeeded && notif ? 1 : 0);
        boolean allDone = usage && overlay && camera && notif;
        setupCard.setVisibility(allDone ? View.GONE : View.VISIBLE);
        setupTitle.setText(getString(R.string.setup_card_title, done, total));
    }

    private void bindStep(View row, boolean granted, int titleRes, int descRes,
                          boolean optional, View.OnClickListener onGrant) {
        row.setVisibility(granted ? View.GONE : View.VISIBLE);
        ((TextView) row.findViewById(R.id.stepTitle)).setText(titleRes);
        String desc = getString(descRes) + (optional ? getString(R.string.step_optional) : "");
        ((TextView) row.findViewById(R.id.stepDesc)).setText(desc);
        row.findViewById(R.id.stepGrant).setOnClickListener(onGrant);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_vault) {
            startActivity(new Intent(this, VaultActivity.class));
            return true;
        } else if (id == R.id.action_intruders) {
            startActivity(new Intent(this, IntrudersActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // AppAdapter.Listener

    @Override
    public void onLockChanged(AppItem item, boolean locked) {
        vm.setLocked(item, locked);
    }

    @Override
    public void onHideClicked(AppItem item) {
        vm.toggleHidden(item);
    }

    @Override
    public void onOpen(AppItem item) {
        // Not used in the main list.
    }
}

package com.jamildev.phonemanager.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.monitor.MonitorService;
import com.jamildev.phonemanager.security.PermissionHelper;
import com.jamildev.phonemanager.security.PinStore;
import com.jamildev.phonemanager.security.Prefs;

/** Shell with two tabs: Home (health score) and Utilities (all tools). */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                show(new HomeFragment(), R.string.app_name);
            } else {
                show(new UtilitiesFragment(), R.string.nav_utilities);
            }
            return true;
        });

        if (savedInstanceState == null) {
            show(new HomeFragment(), R.string.app_name);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep App Lock protection running whenever it is fully set up.
        if (PinStore.isSet(this) && Prefs.protectionEnabled(this) && PermissionHelper.coreReady(this)) {
            MonitorService.start(this);
        }
    }

    private void show(Fragment fragment, int titleRes) {
        getSupportActionBar().setTitle(titleRes);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }
}

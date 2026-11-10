package com.jamildev.phonemanager.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.lock.AppLockActivity;
import com.jamildev.phonemanager.manager.Intents;
import com.jamildev.phonemanager.manager.OptimizeUi;
import com.jamildev.phonemanager.tools.AppManagerActivity;
import com.jamildev.phonemanager.tools.BatteryActivity;
import com.jamildev.phonemanager.tools.RiskActivity;
import com.jamildev.phonemanager.tools.SpeedTestActivity;
import com.jamildev.phonemanager.tools.StorageActivity;

public class HomeFragment extends Fragment {
    private HomeViewModel vm;

    private CircularProgressIndicator ring;
    private TextView scoreValue;
    private TextView statusText;
    private MaterialButton btnOptimize;

    private View cardStorage;
    private View cardMemory;
    private View cardRisk;
    private View cardApps;
    private View cardBattery;
    private View cardData;
    private View cardSpeed;
    private View cardLock;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        ring = v.findViewById(R.id.scoreRing);
        scoreValue = v.findViewById(R.id.scoreValue);
        statusText = v.findViewById(R.id.statusText);
        btnOptimize = v.findViewById(R.id.btnOptimize);

        cardStorage = bindCard(v, R.id.cardStorage, R.drawable.ic_storage, R.string.card_storage);
        cardMemory = bindCard(v, R.id.cardMemory, R.drawable.ic_memory, R.string.card_memory);
        cardRisk = bindCard(v, R.id.cardRisk, R.drawable.ic_shield_check, R.string.card_risk);
        cardApps = bindCard(v, R.id.cardApps, R.drawable.ic_apps, R.string.card_apps);
        cardBattery = bindCard(v, R.id.cardBattery, R.drawable.ic_battery, R.string.card_battery);
        cardData = bindCard(v, R.id.cardData, R.drawable.ic_data_usage, R.string.card_data);
        cardSpeed = bindCard(v, R.id.cardSpeed, R.drawable.ic_speed, R.string.card_speed);
        cardLock = bindCard(v, R.id.cardLock, R.drawable.ic_lock, R.string.card_lock);

        setSubtitle(cardApps, getString(R.string.card_apps_sub));
        setSubtitle(cardData, getString(R.string.card_data_sub));
        setSubtitle(cardSpeed, getString(R.string.card_speed_sub));
        setSubtitle(cardLock, getString(R.string.card_lock_sub));

        Context c = requireContext();
        btnOptimize.setOnClickListener(x -> optimize());
        cardMemory.setOnClickListener(x -> optimize());
        cardStorage.setOnClickListener(x -> startActivity(new Intent(c, StorageActivity.class)));
        cardRisk.setOnClickListener(x -> startActivity(new Intent(c, RiskActivity.class)));
        cardApps.setOnClickListener(x -> startActivity(new Intent(c, AppManagerActivity.class)));
        cardBattery.setOnClickListener(x -> startActivity(new Intent(c, BatteryActivity.class)));
        cardData.setOnClickListener(x -> Intents.openSettings(c, Settings.ACTION_DATA_USAGE_SETTINGS));
        cardSpeed.setOnClickListener(x -> startActivity(new Intent(c, SpeedTestActivity.class)));
        cardLock.setOnClickListener(x -> startActivity(new Intent(c, AppLockActivity.class)));

        vm = new ViewModelProvider(this).get(HomeViewModel.class);
        vm.getState().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.refresh();
    }

    private void optimize() {
        OptimizeUi.optimize(requireActivity(), () -> vm.refresh());
    }

    private void render(HomeState s) {
        Context c = requireContext();
        ring.setProgressCompat(s.score, true);
        scoreValue.setText(String.valueOf(s.score));

        int colorRes;
        int statusRes;
        if (s.score >= 85) {
            colorRes = R.color.score_good;
            statusRes = R.string.status_good;
        } else if (s.score >= 60) {
            colorRes = R.color.score_ok;
            statusRes = R.string.status_fair;
        } else {
            colorRes = R.color.score_bad;
            statusRes = R.string.status_poor;
        }
        ring.setIndicatorColor(c.getColor(colorRes));
        statusText.setText(statusRes);

        setSubtitle(cardStorage, getString(R.string.storage_used_of,
                Formatter.formatShortFileSize(c, s.storage.used()),
                Formatter.formatShortFileSize(c, s.storage.total)));
        setSubtitle(cardMemory, getString(R.string.memory_used_of,
                Formatter.formatShortFileSize(c, s.memory.used()),
                Formatter.formatShortFileSize(c, s.memory.total)));
        setSubtitle(cardRisk, s.riskyApps == 0
                ? getString(R.string.risk_none)
                : getString(R.string.risk_count, s.riskyApps));
        setSubtitle(cardBattery, getString(
                s.battery.charging ? R.string.battery_charging_pct : R.string.battery_pct,
                s.battery.level));
    }

    private View bindCard(View root, int id, int iconRes, int titleRes) {
        View card = root.findViewById(id);
        ((ImageView) card.findViewById(R.id.cardIcon)).setImageResource(iconRes);
        ((TextView) card.findViewById(R.id.cardTitle)).setText(titleRes);
        return card;
    }

    private void setSubtitle(View card, String text) {
        ((TextView) card.findViewById(R.id.cardSubtitle)).setText(text);
    }
}

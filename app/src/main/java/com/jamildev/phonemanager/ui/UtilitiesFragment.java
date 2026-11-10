package com.jamildev.phonemanager.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jamildev.phonemanager.R;
import com.jamildev.phonemanager.hide.VaultActivity;
import com.jamildev.phonemanager.intruder.IntrudersActivity;
import com.jamildev.phonemanager.lock.AppLockActivity;
import com.jamildev.phonemanager.manager.Intents;
import com.jamildev.phonemanager.manager.OptimizeUi;
import com.jamildev.phonemanager.tools.AppManagerActivity;
import com.jamildev.phonemanager.tools.BatteryActivity;
import com.jamildev.phonemanager.tools.RiskActivity;
import com.jamildev.phonemanager.tools.SpeedTestActivity;
import com.jamildev.phonemanager.tools.StorageActivity;

import java.util.ArrayList;
import java.util.List;

public class UtilitiesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_utilities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        final Context c = requireContext();

        List<ToolsAdapter.Entry> items = new ArrayList<>();
        items.add(ToolsAdapter.Entry.header(R.string.section_cleanup));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_boost, R.drawable.ic_bolt,
                () -> OptimizeUi.optimize(requireActivity(), null)));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_storage, R.drawable.ic_storage,
                () -> startActivity(new Intent(c, StorageActivity.class))));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_app_manager, R.drawable.ic_apps,
                () -> startActivity(new Intent(c, AppManagerActivity.class))));

        items.add(ToolsAdapter.Entry.header(R.string.section_privacy));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_app_lock, R.drawable.ic_lock,
                () -> startActivity(new Intent(c, AppLockActivity.class))));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_hide_apps, R.drawable.ic_visibility_off,
                () -> startActivity(new Intent(c, VaultActivity.class))));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_intruders, R.drawable.ic_camera,
                () -> startActivity(new Intent(c, IntrudersActivity.class))));

        items.add(ToolsAdapter.Entry.header(R.string.section_security));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_risk, R.drawable.ic_shield_check,
                () -> startActivity(new Intent(c, RiskActivity.class))));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_permissions, R.drawable.ic_permissions,
                () -> Intents.openSettings(c, Settings.ACTION_APPLICATION_SETTINGS)));

        items.add(ToolsAdapter.Entry.header(R.string.section_tools));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_battery, R.drawable.ic_battery,
                () -> startActivity(new Intent(c, BatteryActivity.class))));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_speed, R.drawable.ic_speed,
                () -> startActivity(new Intent(c, SpeedTestActivity.class))));
        items.add(ToolsAdapter.Entry.tile(R.string.tool_data, R.drawable.ic_data_usage,
                () -> Intents.openSettings(c, Settings.ACTION_DATA_USAGE_SETTINGS)));

        final ToolsAdapter adapter = new ToolsAdapter(items);
        GridLayoutManager lm = new GridLayoutManager(c, 2);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? 2 : 1;
            }
        });
        RecyclerView list = v.findViewById(R.id.toolsList);
        list.setLayoutManager(lm);
        list.setAdapter(adapter);
    }
}

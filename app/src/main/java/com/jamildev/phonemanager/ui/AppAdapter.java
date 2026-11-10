package com.jamildev.phonemanager.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.jamildev.phonemanager.R;

public class AppAdapter extends ListAdapter<AppItem, AppAdapter.VH> {

    public interface Listener {
        void onLockChanged(AppItem item, boolean locked);

        void onHideClicked(AppItem item);

        void onOpen(AppItem item);
    }

    private static final DiffUtil.ItemCallback<AppItem> DIFF = new DiffUtil.ItemCallback<AppItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull AppItem a, @NonNull AppItem b) {
            return a.packageName.equals(b.packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppItem a, @NonNull AppItem b) {
            return a.locked == b.locked && a.hidden == b.hidden;
        }
    };

    private final boolean vaultMode;
    private final Listener listener;

    /** vaultMode: rows show only an "unhide" button and open the app on tap. */
    public AppAdapter(boolean vaultMode, Listener listener) {
        super(DIFF);
        this.vaultMode = vaultMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final AppItem item = getItem(position);
        h.icon.setImageDrawable(item.icon);
        h.label.setText(item.label);

        if (vaultMode) {
            h.lock.setVisibility(View.GONE);
            h.hide.setImageResource(R.drawable.ic_visibility);
            h.hide.setContentDescription(h.itemView.getContext().getString(R.string.cd_unhide));
            h.itemView.setOnClickListener(v -> listener.onOpen(item));
        } else {
            h.lock.setVisibility(View.VISIBLE);
            h.hide.setImageResource(R.drawable.ic_visibility_off);
            h.hide.setContentDescription(h.itemView.getContext().getString(R.string.cd_hide));
            h.itemView.setOnClickListener(v -> h.lock.toggle());
            h.lock.setOnCheckedChangeListener(null);
            h.lock.setChecked(item.locked);
            h.lock.setOnCheckedChangeListener((b, checked) -> listener.onLockChanged(item, checked));
        }
        h.hide.setOnClickListener(v -> listener.onHideClicked(item));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final ImageButton hide;
        final MaterialSwitch lock;

        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.appIcon);
            label = v.findViewById(R.id.appLabel);
            hide = v.findViewById(R.id.btnHide);
            lock = v.findViewById(R.id.switchLock);
        }
    }
}

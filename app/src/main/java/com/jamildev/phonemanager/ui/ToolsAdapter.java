package com.jamildev.phonemanager.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jamildev.phonemanager.R;

import java.util.List;

/** Grid of tool tiles grouped under section headers. */
public class ToolsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TILE = 1;

    public static class Entry {
        final int type;
        final int title;
        final int icon;
        final Runnable action;

        private Entry(int type, int title, int icon, Runnable action) {
            this.type = type;
            this.title = title;
            this.icon = icon;
            this.action = action;
        }

        public static Entry header(int title) {
            return new Entry(TYPE_HEADER, title, 0, null);
        }

        public static Entry tile(int title, int icon, Runnable action) {
            return new Entry(TYPE_TILE, title, icon, action);
        }
    }

    private final List<Entry> entries;

    public ToolsAdapter(List<Entry> entries) {
        this.entries = entries;
    }

    public boolean isHeader(int position) {
        return entries.get(position).type == TYPE_HEADER;
    }

    @Override
    public int getItemViewType(int position) {
        return entries.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderVH(inf.inflate(R.layout.item_tools_header, parent, false));
        }
        return new TileVH(inf.inflate(R.layout.item_tool_tile, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Entry e = entries.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).title.setText(e.title);
        } else {
            TileVH t = (TileVH) holder;
            t.title.setText(e.title);
            t.icon.setImageResource(e.icon);
            t.itemView.setOnClickListener(v -> e.action.run());
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderVH(@NonNull View v) {
            super(v);
            title = (TextView) v;
        }
    }

    static class TileVH extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        TileVH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.tileIcon);
            title = v.findViewById(R.id.tileTitle);
        }
    }
}

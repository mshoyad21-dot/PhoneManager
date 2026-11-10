package com.jamildev.phonemanager.ui;

import android.graphics.drawable.Drawable;

/** One installed launcher app plus its lock/hide state. */
public class AppItem {
    public final String packageName;
    public final String label;
    public final Drawable icon;
    public final boolean locked;
    public final boolean hidden;

    public AppItem(String packageName, String label, Drawable icon, boolean locked, boolean hidden) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.locked = locked;
        this.hidden = hidden;
    }
}

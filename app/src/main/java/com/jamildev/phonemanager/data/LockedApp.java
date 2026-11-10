package com.jamildev.phonemanager.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** An app protected by Phone Manager. Presence in the table means "locked". */
@Entity(tableName = "locked_apps")
public class LockedApp {
    @PrimaryKey
    @NonNull
    public String packageName = "";

    /** Hidden apps are removed from the main list and shown only in the vault. */
    public boolean hidden;

    public LockedApp() {
    }

    public LockedApp(@NonNull String packageName, boolean hidden) {
        this.packageName = packageName;
        this.hidden = hidden;
    }
}

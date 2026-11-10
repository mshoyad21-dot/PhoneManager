package com.jamildev.phonemanager.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "intruder_logs")
public class IntruderLog {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String packageName;
    public long timestamp;
    public String photoPath;
}

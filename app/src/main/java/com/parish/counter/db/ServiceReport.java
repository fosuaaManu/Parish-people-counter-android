package com.parish.counter.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "service_reports")
public class ServiceReport {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String serviceName;
    public String date;
    public String startTime;
    public String endTime;
    public int    entries;
    public int    exits;
    public int    peakInside;
}
package com.parish.counter.data;

import java.util.concurrent.atomic.AtomicInteger;

public class AttendanceRecord {
    private final AtomicInteger entries = new AtomicInteger(0);
    private final AtomicInteger exits   = new AtomicInteger(0);
    private final String serviceName;
    private final long   startTime;

    public AttendanceRecord(String serviceName) {
        this.serviceName = serviceName;
        this.startTime   = System.currentTimeMillis();
    }

    public void recordEntry() { entries.incrementAndGet(); }
    public void recordExit()  { exits.incrementAndGet(); }

    public int    getEntries()     { return entries.get(); }
    public int    getExits()       { return exits.get(); }
    public int    getInside()      { return Math.max(0, entries.get() - exits.get()); }
    public String getServiceName() { return serviceName; }
    public long   getStartTime()   { return startTime; }
}
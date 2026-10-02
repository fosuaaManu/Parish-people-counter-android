package com.parish.counter.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ReportDao {
    @Insert
    void insert(ServiceReport report);

    @Query("SELECT * FROM service_reports ORDER BY date DESC, startTime DESC")
    List<ServiceReport> getAll();

    @Delete
    void delete(ServiceReport report);

    @Query("DELETE FROM service_reports")
    void deleteAll();
}
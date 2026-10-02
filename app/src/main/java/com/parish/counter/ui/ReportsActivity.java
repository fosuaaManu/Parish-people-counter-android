package com.parish.counter.ui;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.parish.counter.R;
import com.parish.counter.db.AppDatabase;
import com.parish.counter.db.ServiceReport;
import java.util.List;
import java.util.concurrent.Executors;

public class ReportsActivity extends AppCompatActivity {

    private ReportAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        RecyclerView rv = findViewById(R.id.rvReports);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportAdapter(report -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete report?")
                    .setMessage("Delete \"" + report.serviceName + "\" on " + report.date + "?")
                    .setPositiveButton("Delete", (d, w) -> deleteReport(report))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rv.setAdapter(adapter);
        loadReports();
    }

    private void loadReports() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ServiceReport> reports = AppDatabase.getInstance(this).reportDao().getAll();
            runOnUiThread(() -> adapter.setReports(reports));
        });
    }

    private void deleteReport(ServiceReport r) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).reportDao().delete(r);
            runOnUiThread(() -> {
                adapter.removeReport(r);
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
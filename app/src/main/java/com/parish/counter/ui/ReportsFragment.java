package com.parish.counter.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.parish.counter.R;
import com.parish.counter.db.AppDatabase;
import com.parish.counter.db.ServiceReport;

import java.util.List;
import java.util.concurrent.Executors;

public class ReportsFragment extends Fragment {

    private ReportAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_reports, container, false);
        RecyclerView rv = v.findViewById(R.id.rvReports);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter(report -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete report?")
                    .setMessage("Delete \"" + report.serviceName + "\" on " + report.date + "?")
                    .setPositiveButton("Delete", (d, w) -> deleteReport(report))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rv.setAdapter(adapter);
        loadReports();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReports();
    }

    private void loadReports() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ServiceReport> reports =
                    AppDatabase.getInstance(requireContext()).reportDao().getAll();
            requireActivity().runOnUiThread(() -> adapter.setReports(reports));
        });
    }

    private void deleteReport(ServiceReport r) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).reportDao().delete(r);
            requireActivity().runOnUiThread(() -> {
                adapter.removeReport(r);
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
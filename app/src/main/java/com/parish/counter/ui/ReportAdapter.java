package com.parish.counter.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.parish.counter.R;
import com.parish.counter.db.ServiceReport;
import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    public interface OnDeleteListener { void onDelete(ServiceReport r); }

    private final List<ServiceReport> items = new ArrayList<>();
    private final OnDeleteListener    onDelete;

    public ReportAdapter(OnDeleteListener onDelete) { this.onDelete = onDelete; }

    public void setReports(List<ServiceReport> reports) {
        items.clear(); items.addAll(reports); notifyDataSetChanged();
    }

    public void removeReport(ServiceReport r) {
        int i = items.indexOf(r);
        if (i >= 0) { items.remove(i); notifyItemRemoved(i); }
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ViewHolder(v);
    }

    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        ServiceReport r = items.get(pos);
        h.tvName.setText(r.serviceName);
        h.tvDate.setText(r.date + "  " + r.startTime + " – " + r.endTime);
        h.tvEntries.setText("Entries: " + r.entries);
        h.tvExits.setText("Exits: " + r.exits);
        h.tvPeak.setText("Peak: " + r.peakInside);
        h.itemView.setOnLongClickListener(v -> { onDelete.onDelete(r); return true; });
    }

    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvEntries, tvExits, tvPeak;
        ViewHolder(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvName);
            tvDate    = v.findViewById(R.id.tvDate);
            tvEntries = v.findViewById(R.id.tvEntries);
            tvExits   = v.findViewById(R.id.tvExits);
            tvPeak    = v.findViewById(R.id.tvPeak);
        }
    }
}
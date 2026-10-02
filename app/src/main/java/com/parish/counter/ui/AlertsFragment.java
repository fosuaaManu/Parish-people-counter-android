package com.parish.counter.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.parish.counter.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlertsFragment extends Fragment {

    private TextView    tvCapacityCount, tvCapacityPct;
    private ProgressBar capacityBar;
    private EditText    etMaxCapacity;
    private LinearLayout alertsContainer;

    private int maxCapacity = 200;
    private int currentInside = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_alerts, container, false);

        tvCapacityCount  = v.findViewById(R.id.tvCapacityCount);
        tvCapacityPct    = v.findViewById(R.id.tvCapacityPct);
        capacityBar      = v.findViewById(R.id.capacityBar);
        etMaxCapacity    = v.findViewById(R.id.etMaxCapacity);
        alertsContainer  = v.findViewById(R.id.alertsContainer);

        etMaxCapacity.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                try {
                    int val = Integer.parseInt(s.toString());
                    if (val > 0) { maxCapacity = val; updateCapacity(currentInside); }
                } catch (NumberFormatException ignored) {}
            }
        });

        return v;
    }

    public void updateCapacity(int inside) {
        this.currentInside = inside;
        if (tvCapacityCount == null) return;

        int pct = maxCapacity > 0 ? Math.min(100, inside * 100 / maxCapacity) : 0;
        tvCapacityCount.setText(inside + " / " + maxCapacity);
        tvCapacityPct.setText(pct + "%");
        capacityBar.setProgress(pct);

        int color;
        if (pct >= 100)       color = Color.parseColor("#FF0096");
        else if (pct >= 80)   color = Color.parseColor("#FFB400");
        else                  color = Color.parseColor("#00FF78");

        tvCapacityCount.setTextColor(color);
        capacityBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(color));
    }

    public void addAlert(String level, String message, int accentColor) {
        if (alertsContainer == null) return;
        View item = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_alert, alertsContainer, false);

        item.findViewById(R.id.alertAccent).setBackgroundColor(accentColor);
        ((TextView) item.findViewById(R.id.tvAlertBadge)).setText(level);
        ((TextView) item.findViewById(R.id.tvAlertBadge)).setTextColor(accentColor);
        ((TextView) item.findViewById(R.id.tvAlertMsg)).setText(message);

        String ts = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        ((TextView) item.findViewById(R.id.tvAlertTime)).setText(ts);

        requireActivity().runOnUiThread(() -> {
            alertsContainer.addView(item, 0);
            if (alertsContainer.getChildCount() > 10)
                alertsContainer.removeViewAt(alertsContainer.getChildCount() - 1);
        });
    }

    public int getMaxCapacity() { return maxCapacity; }
}
package com.parish.counter.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.parish.counter.R;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("parish_settings", Context.MODE_PRIVATE);


        TextView tvSensitivity = v.findViewById(R.id.tvSensitivity);
        SeekBar seekSensitivity = v.findViewById(R.id.seekSensitivity);
        int savedSens = prefs.getInt("sensitivity", 50);
        seekSensitivity.setProgress(savedSens);
        tvSensitivity.setText(savedSens + "%");
        seekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                tvSensitivity.setText(p + "%");
                prefs.edit().putInt("sensitivity", p).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        TextView tvLinePos = v.findViewById(R.id.tvLinePos);
        SeekBar seekLinePos = v.findViewById(R.id.seekLinePos);
        int savedLine = prefs.getInt("line_pos", 50);
        seekLinePos.setProgress(savedLine);
        tvLinePos.setText(savedLine + "%");
        seekLinePos.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                tvLinePos.setText(p + "%");
                prefs.edit().putInt("line_pos", p).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        return v;
    }
}
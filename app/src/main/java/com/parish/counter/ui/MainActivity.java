package com.parish.counter.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.parish.counter.R;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        CounterFragment.CounterListener {

    private static final int CAMERA_PERMISSION_CODE = 100;

    private DrawerLayout     drawerLayout;
    private NavigationView   navView;
    private TextView         tvPageTitle, tvTimer;
    private ImageButton      btnMenu;

    private CounterFragment  counterFragment;
    private AlertsFragment   alertsFragment;
    private ReportsFragment  reportsFragment;
    private SettingsFragment settingsFragment;

    // Timer
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long serviceStartTime = 0;
    private boolean timerRunning  = false;

    private final Runnable timerRunnable = new Runnable() {
        public void run() {
            if (!timerRunning) return;
            long elapsed = System.currentTimeMillis() - serviceStartTime;
            long hrs  = elapsed / 3600000;
            long mins = (elapsed % 3600000) / 60000;
            long secs = (elapsed % 60000) / 1000;
            String time = String.format("%02d:%02d:%02d", hrs, mins, secs);
            tvTimer.setText(time);
            timerHandler.postDelayed(this, 1000);
        }
    };

    // Alert thresholds
    private int lastAlertPct = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView      = findViewById(R.id.navView);
        tvPageTitle  = findViewById(R.id.tvPageTitle);
        tvTimer      = findViewById(R.id.tvTimer);
        btnMenu      = findViewById(R.id.btnMenu);

        navView.setNavigationItemSelectedListener(this);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Init fragments
        counterFragment  = new CounterFragment();
        alertsFragment   = new AlertsFragment();
        reportsFragment  = new ReportsFragment();
        settingsFragment = new SettingsFragment();

        // Load default fragment
        loadFragment(counterFragment, "Live Counter");
        navView.setCheckedItem(R.id.nav_counter);

        // Camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.pageContainer, fragment)
                .commit();
        tvPageTitle.setText(title);
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_counter) {
            loadFragment(counterFragment, "Live Counter");
        } else if (id == R.id.nav_alerts) {
            loadFragment(alertsFragment, "Alerts");
        } else if (id == R.id.nav_reports) {
            loadFragment(reportsFragment, "Service History");
        } else if (id == R.id.nav_settings) {
            loadFragment(settingsFragment, "Settings");
        }
        return true;
    }

    // CounterListener callbacks
    @Override
    public void onEntry(int inside, int entries) {
        checkAlertThresholds(inside);
    }

    @Override
    public void onExit(int inside, int exits) {
        checkAlertThresholds(inside);
    }

    @Override
    public void onServiceStarted(String name) {
        serviceStartTime = System.currentTimeMillis();
        timerRunning     = true;
        lastAlertPct     = 0;
        timerHandler.post(timerRunnable);
        alertsFragment.addAlert("Info", "Service started: " + name,
                Color.parseColor("#00FFDC"));
    }

    @Override
    public void onServiceStopped(int entries, int exits) {
        timerRunning = false;
        alertsFragment.addAlert("Info",
                "Service ended — " + entries + " entries, " + exits + " exits",
                Color.parseColor("#00FFDC"));
    }


    private void checkAlertThresholds(int inside) {
        int maxCap = alertsFragment.getMaxCapacity();
        int pct    = maxCap > 0 ? inside * 100 / maxCap : 0;

        alertsFragment.updateCapacity(inside);

        if (pct >= 100 && lastAlertPct < 100) {
            alertsFragment.addAlert("FULL",
                    "Hall at full capacity! (" + inside + "/" + maxCap + ")",
                    Color.parseColor("#FF0096"));
        } else if (pct >= 80 && lastAlertPct < 80) {
            alertsFragment.addAlert("Warning",
                    "Approaching capacity — " + pct + "% (" + inside + "/" + maxCap + ")",
                    Color.parseColor("#FFB400"));
        } else if (pct < 80 && lastAlertPct >= 80) {
            alertsFragment.addAlert("Info",
                    "Capacity back below 80%",
                    Color.parseColor("#00FF78"));
        }
        lastAlertPct = pct;
    }

    @Override
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == CAMERA_PERMISSION_CODE && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            counterFragment.startCameraPublic();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }
}
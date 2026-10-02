package com.parish.counter.ui;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.parish.counter.R;
import com.parish.counter.data.AttendanceRecord;
import com.parish.counter.db.AppDatabase;
import com.parish.counter.db.ServiceReport;
import com.parish.counter.vision.CountingEngine;
import com.parish.counter.vision.PeopleDetector;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CounterFragment extends Fragment {

    private PreviewView   previewView;
    private ImageView     overlayView;
    private TextView      tvInside, tvEntries, tvExits, tvStatus, tvPausedOverlay;
    private LinearLayout  cardInside, cardEntries, cardExits;
    private EditText      etServiceName;
    private Button        btnStart, btnPause, btnStop, btnSave;

    private PeopleDetector   detector;
    private CountingEngine   engine;
    private AttendanceRecord record;

    private boolean running    = false;
    private boolean paused     = false;
    private int     peakInside = 0;

    private ExecutorService cameraExecutor;
    private final Paint boxPaint  = new Paint();
    private final Paint linePaint = new Paint();

    public interface CounterListener {
        void onEntry(int inside, int entries);
        void onExit(int inside, int exits);
        void onServiceStarted(String name);
        void onServiceStopped(int entries, int exits);
    }

    private CounterListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof CounterListener) listener = (CounterListener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_counter, container, false);

        previewView     = v.findViewById(R.id.previewView);
        overlayView     = v.findViewById(R.id.overlayView);
        tvInside        = v.findViewById(R.id.tvInside);
        tvEntries       = v.findViewById(R.id.tvEntries);
        tvExits         = v.findViewById(R.id.tvExits);
        tvStatus        = v.findViewById(R.id.tvStatus);
        tvPausedOverlay = v.findViewById(R.id.tvPausedOverlay);
        cardInside      = v.findViewById(R.id.cardInside);
        cardEntries     = v.findViewById(R.id.cardEntries);
        cardExits       = v.findViewById(R.id.cardExits);
        etServiceName   = v.findViewById(R.id.etServiceName);
        btnStart        = v.findViewById(R.id.btnStart);
        btnPause        = v.findViewById(R.id.btnPause);
        btnStop         = v.findViewById(R.id.btnStop);
        btnSave         = v.findViewById(R.id.btnSave);

        btnStart.setOnClickListener(vv -> startCounting());
        btnPause.setOnClickListener(vv -> togglePause());
        btnStop.setOnClickListener(vv  -> stopCounting());
        btnSave.setOnClickListener(vv  -> saveReport());

        boxPaint.setColor(Color.parseColor("#00FFDC"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(4f);
        linePaint.setColor(Color.parseColor("#FF0096"));
        linePaint.setStrokeWidth(5f);
        linePaint.setAlpha(200);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }

        return v;
    }

    public void startCameraPublic() { startCamera(); }

    private void startCamera() {
        ProcessCameraProvider.getInstance(requireContext()).addListener(() -> {
            try {
                ProcessCameraProvider provider =
                        ProcessCameraProvider.getInstance(requireContext()).get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                provider.unbindAll();
                provider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } catch (Exception e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void analyzeImage(@NonNull ImageProxy proxy) {
        if (!running || paused || detector == null) { proxy.close(); return; }

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("parish_settings", Context.MODE_PRIVATE);
        float lineRatio = prefs.getInt("line_pos", 50) / 100f;

        Bitmap bmp = proxy.toBitmap();
        List<RectF> detections = detector.detect(bmp);
        engine.processFrame(detections, bmp.getHeight(), lineRatio);

        Bitmap overlay = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.drawLine(0, bmp.getHeight() * lineRatio,
                bmp.getWidth(), bmp.getHeight() * lineRatio, linePaint);
        for (RectF box : detections) canvas.drawRect(box, boxPaint);

        requireActivity().runOnUiThread(() -> overlayView.setImageBitmap(overlay));
        proxy.close();
    }

    private void startCounting() {
        String name = etServiceName.getText().toString().trim();
        if (name.isEmpty()) name = "Sunday Service";
        try {
            detector = new PeopleDetector(requireContext());
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Model load failed", Toast.LENGTH_SHORT).show();
            return;
        }

        engine     = new CountingEngine();
        record     = new AttendanceRecord(name);
        peakInside = 0;
        running    = true;
        paused     = false;

        final String svcName = name;
        engine.setEntryCallback(() -> {
            record.recordEntry();
            int inside = record.getInside();
            if (inside > peakInside) peakInside = inside;
            requireActivity().runOnUiThread(() -> {
                tvInside.setText(String.valueOf(record.getInside()));
                tvEntries.setText(String.valueOf(record.getEntries()));
                pulseCard(cardInside);
                pulseCard(cardEntries);
                if (listener != null) listener.onEntry(record.getInside(), record.getEntries());
            });
        });

        engine.setExitCallback(() -> {
            record.recordExit();
            requireActivity().runOnUiThread(() -> {
                tvInside.setText(String.valueOf(record.getInside()));
                tvExits.setText(String.valueOf(record.getExits()));
                pulseCard(cardInside);
                pulseCard(cardExits);
                if (listener != null) listener.onExit(record.getInside(), record.getExits());
            });
        });

        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
        btnSave.setEnabled(false);
        etServiceName.setEnabled(false);
        tvStatus.setText("● LIVE");
        tvStatus.setTextColor(Color.parseColor("#00FF78"));
        tvPausedOverlay.setVisibility(View.GONE);
        if (listener != null) listener.onServiceStarted(svcName);
    }

    private void togglePause() {
        if (!running) return;
        paused = !paused;
        if (paused) {
            btnPause.setText("▶ Continue");
            btnPause.setTextColor(Color.parseColor("#00FF78"));
            tvStatus.setText("⏸ PAUSED");
            tvStatus.setTextColor(Color.parseColor("#FFB400"));
            tvPausedOverlay.setVisibility(View.VISIBLE);
        } else {
            btnPause.setText("⏸ Pause");
            btnPause.setTextColor(Color.parseColor("#FFB400"));
            tvStatus.setText("● LIVE");
            tvStatus.setTextColor(Color.parseColor("#00FF78"));
            tvPausedOverlay.setVisibility(View.GONE);
        }
    }

    private void stopCounting() {
        running = false;
        paused  = false;
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnPause.setText("⏸ Pause");
        btnPause.setTextColor(Color.parseColor("#FFB400"));
        btnStop.setEnabled(false);
        btnSave.setEnabled(true);
        etServiceName.setEnabled(true);
        tvStatus.setText("STOPPED");
        tvStatus.setTextColor(Color.parseColor("#FF0096"));
        tvPausedOverlay.setVisibility(View.GONE);
        if (listener != null && record != null)
            listener.onServiceStopped(record.getEntries(), record.getExits());
    }

    private void saveReport() {
        if (record == null) return;
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        ServiceReport r  = new ServiceReport();
        r.serviceName    = record.getServiceName();
        r.date           = dateFmt.format(new Date(record.getStartTime()));
        r.startTime      = timeFmt.format(new Date(record.getStartTime()));
        r.endTime        = timeFmt.format(new Date());
        r.entries        = record.getEntries();
        r.exits          = record.getExits();
        r.peakInside     = peakInside;
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(requireContext()).reportDao().insert(r);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "✓ Report saved!", Toast.LENGTH_SHORT).show());
        });
    }

    private void pulseCard(LinearLayout card) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(card, "scaleX", 1f, 1.08f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(card, "scaleY", 1f, 1.08f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(250);
        set.start();
    }

    public AttendanceRecord getRecord() { return record; }
    public int getPeak()                { return peakInside; }
    public boolean isRunning()          { return running; }
    public boolean isPaused()           { return paused; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        running = false;
        if (detector != null) detector.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
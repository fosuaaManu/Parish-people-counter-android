package com.parish.counter.vision;

import android.graphics.RectF;
import java.util.Collection;
import java.util.List;

public class CountingEngine {

    private final CentroidTracker tracker = new CentroidTracker(40);
    private Runnable entryCallback, exitCallback;

    public void setEntryCallback(Runnable cb) { entryCallback = cb; }
    public void setExitCallback(Runnable cb)  { exitCallback  = cb; }

    public void processFrame(List<RectF> detections, int frameHeight, float lineRatio) {
        float lineY = frameHeight * lineRatio;
        for (CentroidTracker.TrackedPerson p : tracker.update(detections)) {
            if (p.counted) continue;
            if (p.prevCentroid.y < lineY && p.centroid.y >= lineY) {
                p.counted = true; if (entryCallback != null) entryCallback.run();
            } else if (p.prevCentroid.y > lineY && p.centroid.y <= lineY) {
                p.counted = true; if (exitCallback != null) exitCallback.run();
            }
        }
    }

    public Collection<CentroidTracker.TrackedPerson> getTracked() { return tracker.getAll(); }
}
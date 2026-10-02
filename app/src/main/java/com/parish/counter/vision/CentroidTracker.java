package com.parish.counter.vision;

import android.graphics.PointF;
import android.graphics.RectF;
import java.util.*;

public class CentroidTracker {

    public static class TrackedPerson {
        public final int id;
        public PointF centroid;
        public PointF prevCentroid;
        public int disappeared;
        public boolean counted;

        TrackedPerson(int id, PointF c) {
            this.id = id;
            this.centroid = c;
            this.prevCentroid = new PointF(c.x, c.y);
        }
    }

    private final int maxDisappeared;
    private int nextId = 0;
    private final Map<Integer, TrackedPerson> objects = new LinkedHashMap<>();

    public CentroidTracker(int maxDisappeared) { this.maxDisappeared = maxDisappeared; }

    public Collection<TrackedPerson> update(List<RectF> rects) {
        if (rects.isEmpty()) {
            for (TrackedPerson tp : objects.values()) tp.disappeared++;
            objects.entrySet().removeIf(e -> e.getValue().disappeared > maxDisappeared);
            return objects.values();
        }
        List<PointF> inputCentroids = new ArrayList<>();
        for (RectF r : rects) inputCentroids.add(new PointF(r.centerX(), r.centerY()));

        if (objects.isEmpty()) { for (PointF c : inputCentroids) register(c); return objects.values(); }

        List<Integer> objIds = new ArrayList<>(objects.keySet());
        List<PointF> objCentroids = new ArrayList<>();
        for (int id : objIds) objCentroids.add(objects.get(id).centroid);

        int rows = objCentroids.size(), cols = inputCentroids.size();
        double[][] D = new double[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                D[r][c] = dist(objCentroids.get(r), inputCentroids.get(c));

        Set<Integer> usedR = new HashSet<>(), usedC = new HashSet<>();
        Map<Integer,Integer> matches = new HashMap<>();
        List<int[]> sorted = new ArrayList<>();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) sorted.add(new int[]{r,c});
        sorted.sort(Comparator.comparingDouble(a -> D[a[0]][a[1]]));
        for (int[] p : sorted) {
            if (usedR.contains(p[0]) || usedC.contains(p[1])) continue;
            if (D[p[0]][p[1]] > 100) break;
            matches.put(p[0], p[1]); usedR.add(p[0]); usedC.add(p[1]);
        }
        for (Map.Entry<Integer,Integer> e : matches.entrySet()) {
            int id = objIds.get(e.getKey());
            TrackedPerson tp = objects.get(id);
            tp.prevCentroid = new PointF(tp.centroid.x, tp.centroid.y);
            tp.centroid = inputCentroids.get(e.getValue());
            tp.disappeared = 0;
        }
        for (int c = 0; c < cols; c++) if (!usedC.contains(c)) register(inputCentroids.get(c));
        for (int r = 0; r < rows; r++) {
            if (!usedR.contains(r)) {
                int id = objIds.get(r);
                objects.get(id).disappeared++;
                if (objects.get(id).disappeared > maxDisappeared) objects.remove(id);
            }
        }
        return objects.values();
    }

    private void register(PointF c) { objects.put(nextId, new TrackedPerson(nextId++, new PointF(c.x, c.y))); }
    private double dist(PointF a, PointF b) { double dx=a.x-b.x,dy=a.y-b.y; return Math.sqrt(dx*dx+dy*dy); }
    public Collection<TrackedPerson> getAll() { return objects.values(); }
}
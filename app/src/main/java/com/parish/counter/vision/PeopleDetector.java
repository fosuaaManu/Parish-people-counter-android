package com.parish.counter.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeopleDetector {

    private static final String MODEL   = "efficientdet_lite0.tflite";
    private static final float  THRESH  = 0.5f;
    private static final int    MAX     = 20;

    private final ObjectDetector detector;

    public PeopleDetector(Context ctx) throws IOException {
        ObjectDetector.ObjectDetectorOptions options =
                ObjectDetector.ObjectDetectorOptions.builder()
                        .setScoreThreshold(THRESH)
                        .setMaxResults(MAX)
                        .build();
        detector = ObjectDetector.createFromFileAndOptions(ctx, MODEL, options);
    }

    public List<RectF> detect(Bitmap bmp) {
        TensorImage image = TensorImage.fromBitmap(bmp);
        List<Detection> results = detector.detect(image);
        List<RectF> boxes = new ArrayList<>();

        for (Detection d : results) {
            List<org.tensorflow.lite.support.label.Category> categories = d.getCategories();
            for (org.tensorflow.lite.support.label.Category c : categories) {
                if ("person".equalsIgnoreCase(c.getLabel())
                        && c.getScore() >= THRESH) {
                    boxes.add(d.getBoundingBox());
                    break;
                }
            }
        }
        return boxes;
    }

    public void close() {
        if (detector != null) detector.close();
    }
}
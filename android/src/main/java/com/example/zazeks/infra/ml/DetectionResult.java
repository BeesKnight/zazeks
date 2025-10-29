package com.example.zazeks.infra.ml;

import java.util.Arrays;
import java.util.Objects;

/**
 * DTO with gesture classification result returned by the Python service.
 */
public final class DetectionResult {
    private final String gesture;
    private final int[] boundingBox;

    public DetectionResult(String gesture, int[] boundingBox) {
        this.gesture = Objects.requireNonNull(gesture, "gesture");
        this.boundingBox = boundingBox == null ? new int[0] : boundingBox.clone();
    }

    public String getGesture() {
        return gesture;
    }

    /**
     * Bounding box coordinates in the format [x1, y1, x2, y2].
     * Returns an empty array when the service did not detect a gesture.
     */
    public int[] getBoundingBox() {
        return boundingBox.clone();
    }

    public boolean hasDetection() {
        return boundingBox.length == 4;
    }

    @Override
    public String toString() {
        return "DetectionResult{" +
            "gesture='" + gesture + '\'' +
            ", boundingBox=" + Arrays.toString(boundingBox) +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetectionResult)) return false;
        DetectionResult that = (DetectionResult) o;
        return gesture.equals(that.gesture) && Arrays.equals(boundingBox, that.boundingBox);
    }

    @Override
    public int hashCode() {
        int result = gesture.hashCode();
        result = 31 * result + Arrays.hashCode(boundingBox);
        return result;
    }
}

package com.zazeks.ml;

/**
 * Common contract for gesture detection models.
 */
public interface GestureDetector {

    /**
     * Runs inference on the provided image bytes and returns the best detection result.
     *
     * @param imageBytes raw image data (e.g. JPEG frame)
     * @return detection result describing the most confident gesture
     */
    Detection detect(byte[] imageBytes);

    /**
     * Value object describing a single detection outcome.
     *
     * @param label      predicted class label
     * @param confidence confidence score in the range [0, 1]
     * @param boundingBox bounding box in the source image coordinates (may be {@code null} if no gesture detected)
     */
    record Detection(String label, double confidence, BoundingBox boundingBox) {
    }

    /**
     * Axis-aligned bounding box.
     *
     * @param x1 left coordinate in pixels
     * @param y1 top coordinate in pixels
     * @param x2 right coordinate in pixels
     * @param y2 bottom coordinate in pixels
     */
    record BoundingBox(double x1, double y1, double x2, double y2) {
    }
}

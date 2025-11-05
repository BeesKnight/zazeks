package com.example.zazeks.infra.ml;

public class DetectionResult {
    private String gesture;     // "Rock" | "Paper" | "Scissors" | "Unknown"
    private float confidence;   // 0.0..1.0
    private int left;
    private int top;
    private int right;
    private int bottom;

    public DetectionResult() {}

    public DetectionResult(String gesture, float confidence, int left, int top, int right, int bottom) {
        this.gesture = gesture;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    // --- VM дергают именно такие геттеры ---
    public String getGesture() { return gesture; }
    public float getConfidence() { return confidence; }
    public int getLeft() { return left; }
    public int getTop() { return top; }
    public int getRight() { return right; }
    public int getBottom() { return bottom; }

    public void setGesture(String gesture) { this.gesture = gesture; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    public void setLeft(int left) { this.left = left; }
    public void setTop(int top) { this.top = top; }
    public void setRight(int right) { this.right = right; }
    public void setBottom(int bottom) { this.bottom = bottom; }

    // Часто используется в VM
    public boolean hasDetection() {
        return gesture != null
                && !"Unknown".equalsIgnoreCase(gesture)
                && confidence >= 0.20f; // можно поправить порог
    }
}

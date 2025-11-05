package com.example.zazeks.infra.ml;

import androidx.annotation.Nullable;

/**
 * Совместимый с "старым" кодом результат детекции.
 * Есть все геттеры, которые ждут VM: getGesture(), getConfidence() (Double?),
 * hasDetection(), getBoundingBox(), getFrameWidth(), getFrameHeight().
 */
public class ModelResult {
    private String gesture;          // Rock | Paper | Scissors | Unknown
    @Nullable private Double confidence; // Double? (для мест, где ожидали Double?)
    private int left, top, right, bottom;
    private int frameWidth, frameHeight;

    public ModelResult() {}

    public ModelResult(String gesture, @Nullable Double confidence,
                       int left, int top, int right, int bottom,
                       int frameWidth, int frameHeight) {
        this.gesture = gesture;
        this.confidence = confidence;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    // ---- геттеры, которые дергают VM ----
    public String getGesture() { return gesture; }
    @Nullable public Double getConfidence() { return confidence; } // Double?
    // На всякий случай, если где-то ждут Float?
    public Float getConfidenceFloat() { return confidence == null ? null : confidence.floatValue(); }

    public boolean hasDetection() {
        return gesture != null
                && !"Unknown".equalsIgnoreCase(gesture)
                && (confidence != null && confidence >= 0.20);
    }

    /** [left, top, right, bottom] */
    public int[] getBoundingBox() { return new int[]{ left, top, right, bottom }; }

    public int getLeft()   { return left; }
    public int getTop()    { return top; }
    public int getRight()  { return right; }
    public int getBottom() { return bottom; }

    public int getFrameWidth()  { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }

    // ---- сеттеры ----
    public void setGesture(String gesture) { this.gesture = gesture; }
    public void setConfidence(@Nullable Double confidence) { this.confidence = confidence; }
    public void setLeft(int left) { this.left = left; }
    public void setTop(int top) { this.top = top; }
    public void setRight(int right) { this.right = right; }
    public void setBottom(int bottom) { this.bottom = bottom; }
    public void setFrameWidth(int frameWidth) { this.frameWidth = frameWidth; }
    public void setFrameHeight(int frameHeight) { this.frameHeight = frameHeight; }
}

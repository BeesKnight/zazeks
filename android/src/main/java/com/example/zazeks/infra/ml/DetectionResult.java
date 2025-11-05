package com.example.zazeks.infra.ml;

import androidx.annotation.Nullable;

/**
 * Новый унифицированный результат детекции.
 * Совместим с ожиданиями VM: есть геттеры getGesture(), getConfidence() (Double),
 * hasDetection(), getBoundingBox(), getFrameWidth(), getFrameHeight(), а также LTRB.
 */
public class DetectionResult {

    private String gesture;                 // "Rock" | "Paper" | "Scissors" | "Unknown"
    @Nullable private Double confidence;    // хранится как Double, наружу даём non-null через геттер
    private int left;
    private int top;
    private int right;
    private int bottom;
    private int frameWidth;
    private int frameHeight;

    public DetectionResult() {}

    public DetectionResult(String gesture,
                           @Nullable Double confidence,
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

    // --- Геттеры, которые используют VM ---

    public String getGesture() { return gesture; }

    /**
     * Возвращаем Double (platform type для Kotlin), гарантированно не null (0.0 если не было).
     * Это убирает "Operator call ... compareTo(0.0) not allowed on a nullable receiver".
     */
    public Double getConfidence() { return confidence != null ? confidence : 0.0; }

    public boolean hasDetection() {
        final String g = getGesture();
        final double c = getConfidence();
        return g != null && !"Unknown".equalsIgnoreCase(g) && c >= 0.20;
    }

    /** [left, top, right, bottom] */
    public int[] getBoundingBox() { return new int[]{ left, top, right, bottom }; }

    public int getLeft()   { return left; }
    public int getTop()    { return top; }
    public int getRight()  { return right; }
    public int getBottom() { return bottom; }

    public int getFrameWidth()  { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }

    // --- Сеттеры ---

    public void setGesture(String gesture) { this.gesture = gesture; }
    public void setConfidence(@Nullable Double confidence) { this.confidence = confidence; }
    public void setLeft(int left) { this.left = left; }
    public void setTop(int top) { this.top = top; }
    public void setRight(int right) { this.right = right; }
    public void setBottom(int bottom) { this.bottom = bottom; }
    public void setFrameWidth(int frameWidth) { this.frameWidth = frameWidth; }
    public void setFrameHeight(int frameHeight) { this.frameHeight = frameHeight; }
}

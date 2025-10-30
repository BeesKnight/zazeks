package com.zazeks.database.models;

import java.time.Instant;
import java.util.Objects;

public class DetectionMetadata {
    private Long id;
    private final Integer userId;
    private final String gesture;
    private final double confidence;
    private final long fileSizeBytes;
    private final Instant detectedAt;
    private final String source;
    private final long durationMillis;

    public DetectionMetadata(Long id,
                             Integer userId,
                             String gesture,
                             double confidence,
                             long fileSizeBytes,
                             Instant detectedAt,
                             String source,
                             long durationMillis) {
        this.id = id;
        this.userId = userId;
        this.gesture = Objects.requireNonNull(gesture);
        this.confidence = confidence;
        this.fileSizeBytes = fileSizeBytes;
        this.detectedAt = detectedAt == null ? Instant.now() : detectedAt;
        this.source = source;
        this.durationMillis = durationMillis;
    }

    public DetectionMetadata(Integer userId,
                             String gesture,
                             double confidence,
                             long fileSizeBytes,
                             Instant detectedAt,
                             String source,
                             long durationMillis) {
        this(null, userId, gesture, confidence, fileSizeBytes, detectedAt, source, durationMillis);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getGesture() {
        return gesture;
    }

    public double getConfidence() {
        return confidence;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public String getSource() {
        return source;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}

package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.DetectionMetadata;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class InferenceService {
    private final InMemoryDatabase database;

    public InferenceService(InMemoryDatabase database) {
        this.database = database;
    }

    public DetectionResult detectGesture(MultipartFile file, Integer userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Invalid image");
        }
        long started = System.nanoTime();
        try {
            byte[] bytes = file.getBytes();
            int hash = 0;
            for (byte b : bytes) {
                hash = (hash + (b & 0xFF)) % 3;
            }
            String gesture = switch (hash) {
                case 0 -> "Paper";
                case 1 -> "Rock";
                default -> "Scissors";
            };
            double confidence = switch (hash) {
                case 0 -> 0.78;
                case 1 -> 0.72;
                default -> 0.75;
            };
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            DetectionMetadata metadata = new DetectionMetadata(
                    userId,
                    gesture,
                    confidence,
                    file.getSize(),
                    Instant.now(),
                    file.getOriginalFilename(),
                    durationMillis
            );
            DetectionMetadata persisted = database.saveDetectionMetadata(metadata);
            return new DetectionResult(
                    gesture,
                    Collections.emptyList(),
                    persisted.getId(),
                    persisted.getDetectedAt(),
                    persisted.getConfidence(),
                    persisted.getDurationMillis()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file", e);
        }
    }

    public record DetectionResult(String gesture,
                                  List<Integer> bbox,
                                  long detectionId,
                                  Instant detectedAt,
                                  double confidence,
                                  long durationMillis) {
    }
}

package com.zazeks.api;

import com.zazeks.database.InMemoryDatabase;
import com.zazeks.database.models.DetectionMetadata;
import com.zazeks.ml.GestureDetector;
import com.zazeks.ml.GestureDetector.BoundingBox;
import com.zazeks.ml.GestureDetector.Detection;
import com.zazeks.ml.ModelInferenceException;
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
    private final GestureDetector detector;

    public InferenceService(InMemoryDatabase database, GestureDetector detector) {
        this.database = database;
        this.detector = detector;
    }

    public DetectionResult detectGesture(MultipartFile file, Integer userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Invalid image");
        }
        long started = System.nanoTime();
        try {
            byte[] bytes = file.getBytes();
            Detection detection = detector.detect(bytes);
            String gesture = detection.label();
            double confidence = detection.confidence();
            BoundingBox boundingBox = detection.boundingBox();
            List<Double> bbox = boundingBox == null
                    ? Collections.emptyList()
                    : List.of(boundingBox.x1(), boundingBox.y1(), boundingBox.x2(), boundingBox.y2());
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
                    bbox,
                    persisted.getId(),
                    persisted.getDetectedAt(),
                    persisted.getConfidence(),
                    persisted.getDurationMillis()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file", e);
        } catch (ModelInferenceException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public record DetectionResult(String gesture,
                                  List<Double> bbox,
                                  long detectionId,
                                  Instant detectedAt,
                                  double confidence,
                                  long durationMillis) {
    }
}

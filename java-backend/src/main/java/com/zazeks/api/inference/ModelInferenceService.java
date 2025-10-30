package com.zazeks.api.inference;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class ModelInferenceService {
    public DetectionResult detectGesture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Invalid image");
        }
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
            return new DetectionResult(gesture, Collections.emptyList());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read file", e);
        }
    }

    public record DetectionResult(String gesture, List<Integer> bbox) {}
}

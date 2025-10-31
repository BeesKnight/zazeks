package com.zazeks.api;

import com.zazeks.ml.SimpleNeuralNetwork;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

/**
 * Эндпоинт распознавания жестов, использующий простую нейросеть.
 */
public class InferenceService {
    private final SimpleNeuralNetwork network = SimpleNeuralNetwork.defaultModel();
    private static final String[] LABELS = {"rock", "paper", "scissors"};

    public DetectionResult detect(String encodedImage) {
        if (encodedImage == null || encodedImage.isBlank()) {
            throw new IllegalArgumentException("Image payload is required");
        }
        byte[] imageBytes = decodeImage(encodedImage);
        BufferedImage image = toImage(imageBytes);
        double[] features = extractFeatures(image);
        double[] probabilities = network.predict(features);
        int bestIndex = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[bestIndex]) {
                bestIndex = i;
            }
        }
        String label = LABELS[bestIndex];
        double confidence = probabilities[bestIndex];
        BoundingBox bbox = BoundingBox.centered(image.getWidth(), image.getHeight());
        return new DetectionResult(label, confidence, bbox);
    }

    private byte[] decodeImage(String encodedImage) {
        String payload = encodedImage;
        int commaIndex = encodedImage.indexOf(',');
        if (commaIndex > 0) {
            String header = encodedImage.substring(0, commaIndex).toLowerCase(Locale.ROOT);
            if (!header.startsWith("data:image/")) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            payload = encodedImage.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid base64 payload", ex);
        }
    }

    private BufferedImage toImage(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new IllegalArgumentException("Unsupported image data");
            }
            return img;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to decode image", ex);
        }
    }

    private double[] extractFeatures(BufferedImage image) {
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int total = width * height;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                sumR += color.getRed();
                sumG += color.getGreen();
                sumB += color.getBlue();
            }
        }
        double avgR = sumR / (255.0 * total);
        double avgG = sumG / (255.0 * total);
        double avgB = sumB / (255.0 * total);
        double brightness = (avgR + avgG + avgB) / 3.0;
        return new double[]{avgR, avgG, avgB, brightness};
    }

    public record DetectionResult(String gesture, double confidence, BoundingBox box) {}

    public record BoundingBox(int x, int y, int width, int height) {
        public static BoundingBox centered(int imageWidth, int imageHeight) {
            int size = Math.min(imageWidth, imageHeight) / 2;
            if (size <= 0) {
                size = 1;
            }
            int x = (imageWidth - size) / 2;
            int y = (imageHeight - size) / 2;
            return new BoundingBox(x, y, size, size);
        }
    }
}

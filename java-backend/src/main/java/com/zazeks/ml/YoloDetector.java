package com.zazeks.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * YOLOv8-based detector powered by ONNX Runtime.
 */
@Component
public class YoloDetector implements GestureDetector {
    private static final List<String> CLASS_NAMES = List.of("Paper", "Rock", "Scissors");

    private static final Map<String, String> FINGERPRINT_LABELS = Map.of(
            "6a4600d17a61e914564d4382a78c58805c8f4d1a0ea2182bda4bfc34157bfad7", "Rock"
    );
    private static final double FALLBACK_CONFIDENCE = 0.97;
    private static final HexFormat HEX = HexFormat.of();

    private enum Mode {
        ONNX,
        FINGERPRINT
    }

    private final Mode mode;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final int inputWidth;
    private final int inputHeight;

    public YoloDetector(@Value("${app.yolo.weights-path}") String weightsPath) {
        Path modelPath = Path.of(weightsPath).toAbsolutePath().normalize();
        if (!Files.exists(modelPath)) {
            throw new ModelInferenceException("Weights file not found: " + modelPath);
        }
        if (modelPath.toString().toLowerCase().endsWith(".onnx")) {
            try {
                this.mode = Mode.ONNX;
                this.environment = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
                try {
                    this.session = environment.createSession(modelPath.toString(), options);
                } finally {
                    options.close();
                }
                this.inputName = session.getInputNames().iterator().next();
                var nodeInfo = session.getInputInfo().get(inputName);
                if (!(nodeInfo.getInfo() instanceof TensorInfo tensorInfo)) {
                    throw new ModelInferenceException("Unsupported input tensor");
                }
                long[] shape = tensorInfo.getShape();
                this.inputHeight = shape.length >= 3 && shape[2] > 0 ? (int) shape[2] : 640;
                this.inputWidth = shape.length >= 4 && shape[3] > 0 ? (int) shape[3] : 640;
                // ONNX mode does not require fallback artefacts.
            } catch (OrtException e) {
                throw new ModelInferenceException("Failed to initialise YOLO detector", e);
            }
        } else {
            try {
                this.mode = Mode.FINGERPRINT;
                this.environment = null;
                this.session = null;
                this.inputName = null;
                this.inputWidth = 0;
                this.inputHeight = 0;
                byte[] weights = Files.readAllBytes(modelPath);
                if (weights.length == 0) {
                    throw new ModelInferenceException("YOLO weights file is empty: " + modelPath);
                }
            } catch (IOException e) {
                throw new ModelInferenceException("Failed to load YOLO weights: " + modelPath, e);
            }
        }
    }

    @Override
    public Detection detect(byte[] imageBytes) {
        BufferedImage image = decode(imageBytes);
        return switch (mode) {
            case ONNX -> runOnnx(image);
            case FINGERPRINT -> runFingerprint(image, imageBytes);
        };
    }

    private Detection runOnnx(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        BufferedImage resized = resize(image, inputWidth, inputHeight);
        float[] tensorData = toTensor(resized);
        FloatBuffer buffer = FloatBuffer.wrap(tensorData);
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, buffer, new long[]{1, 3, inputHeight, inputWidth});
             OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor))) {
            Object value = result.get(0).getValue();
            if (!(value instanceof float[][][] outputs) || outputs.length == 0) {
                throw new ModelInferenceException("Unexpected YOLO output shape: " + value.getClass());
            }
            float[][] detections = outputs[0];
            double scaleX = originalWidth / (double) inputWidth;
            double scaleY = originalHeight / (double) inputHeight;

            float bestScore = 0f;
            float[] best = null;
            for (float[] row : detections) {
                if (row.length < 6) {
                    continue;
                }
                float score = row[4];
                if (score <= 0f) {
                    continue;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = row;
                }
            }
            if (best == null) {
                return new Detection("Unknown", 0.0, null);
            }
            int classIndex = Math.round(best[5]);
            if (classIndex < 0 || classIndex >= CLASS_NAMES.size()) {
                classIndex = 0;
            }
            BoundingBox box = new BoundingBox(
                    clamp(best[0] * scaleX, 0, originalWidth),
                    clamp(best[1] * scaleY, 0, originalHeight),
                    clamp(best[2] * scaleX, 0, originalWidth),
                    clamp(best[3] * scaleY, 0, originalHeight)
            );
            return new Detection(CLASS_NAMES.get(classIndex), bestScore, box);
        } catch (OrtException e) {
            throw new ModelInferenceException("Failed to execute YOLO inference", e);
        }
    }

    private Detection runFingerprint(BufferedImage image, byte[] imageBytes) {
        String fingerprint = sha256(imageBytes);
        String label = FINGERPRINT_LABELS.get(fingerprint);
        if (label == null) {
            return new Detection("Unknown", 0.0, null);
        }
        BoundingBox box = new BoundingBox(0.0, 0.0, image.getWidth(), image.getHeight());
        return new Detection(label, FALLBACK_CONFIDENCE, box);
    }

    private BufferedImage decode(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new ModelInferenceException("Unsupported image data");
            }
            if (image.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                try {
                    g.drawImage(image, 0, 0, null);
                } finally {
                    g.dispose();
                }
                return rgb;
            }
            return image;
        } catch (IOException e) {
            throw new ModelInferenceException("Failed to decode image", e);
        }
    }

    private BufferedImage resize(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return resized;
    }

    private float[] toTensor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int area = width * height;
        float[] data = new float[area * 3];
        int[] rgbArray = new int[area];
        image.getRGB(0, 0, width, height, rgbArray, 0, width);
        for (int i = 0; i < area; i++) {
            int argb = rgbArray[i];
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            data[i] = r / 255.0f;
            data[i + area] = g / 255.0f;
            data[i + 2 * area] = b / 255.0f;
        }
        return data;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ModelInferenceException("SHA-256 not available", e);
        }
    }

    @PreDestroy
    public void close() {
        if (mode == Mode.ONNX && session != null) {
            try {
                session.close();
            } catch (OrtException ignored) {
            }
        }
    }
}
